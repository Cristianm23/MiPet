package com.example.mipet.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mipet.data.model.Resource
import com.example.mipet.data.model.Vet
import com.example.mipet.ui.navigation.Screen
import com.example.mipet.ui.theme.Turquoise
import com.example.mipet.ui.viewmodel.VetViewModel
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VetMapScreen(navController: NavController, viewModel: VetViewModel = viewModel()) {
    val context = LocalContext.current

    val vetsState by viewModel.vetsState.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val selectedVet by viewModel.selectedVet.collectAsState()
    val showRoute by viewModel.showRoute.collectAsState()

    var searchQueryText by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var viewMode by remember { mutableStateOf(0) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchDeviceLocation(context) { lat, lng ->
                viewModel.updateLocationFromGps(lat, lng)
            }
        } else {
            viewModel.updateLocationFromGps(4.6097, -74.0817)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fetchDeviceLocation(context) { lat, lng ->
                viewModel.updateLocationFromGps(lat, lng)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                val items = listOf(
                    Triple("Inicio", Icons.Default.Home, Screen.Home.route),
                    Triple("Mascotas", Icons.Default.Pets, Screen.PetList.route),
                    Triple("Veterinarias", Icons.Default.Place, Screen.VetMap.route),
                    Triple("Asistente", Icons.AutoMirrored.Filled.Chat, Screen.AiAssistant.route)
                )

                items.forEach { (label, icon, route) ->
                    NavigationBarItem(
                        selected = route == Screen.VetMap.route,
                        onClick = { if (route != Screen.VetMap.route) navController.navigate(route) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Turquoise,
                            selectedTextColor = Turquoise,
                            unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val currentLat = userLocation?.first ?: 4.6097
            val currentLng = userLocation?.second ?: -74.0817
            val currentVets = (vetsState as? Resource.Success)?.data ?: emptyList()

            if (viewMode == 0) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowContentAccess = true
                            settings.allowFileAccess = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; MiPetApp/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    println("VetMap Console: [${consoleMessage?.messageLevel()}] ${consoleMessage?.message()}")
                                    return true
                                }
                            }

                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onVetSelected(vetId: String) {
                                    val found = currentVets.find { it.id == vetId }
                                    if (found != null) {
                                        viewModel.selectVet(found)
                                    }
                                }
                            }, "AndroidMap")

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    updateMapData(view, currentLat, currentLng, currentVets, selectedVet, showRoute)
                                }
                            }

                            loadDataWithBaseURL(
                                "https://mt1.google.com",
                                getMapHtmlContent(),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        updateMapData(webView, currentLat, currentLng, currentVets, selectedVet, showRoute)
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 135.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        "$locationLabel - (${currentVets.size} cerca):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (vetsState is Resource.Loading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (currentVets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron veterinarias en esta zona.")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(currentVets) { vet ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectVet(vet) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                vet.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = if (vet.isOpen) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (vet.isOpen) "Abierto" else "Cerrado",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "⭐ ${vet.rating} • ${vet.distanceKm} km • ${vet.openHours}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Turquoise, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(vet.address, style = MaterialTheme.typography.bodyMedium)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = Turquoise, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(vet.phone, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQueryText,
                                onValueChange = { searchQueryText = it },
                                placeholder = { Text("Buscar ciudad (ej. Medellín)...") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (searchQueryText.isNotBlank()) {
                                        viewModel.searchAddress(searchQueryText)
                                    }
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = {
                                if (searchQueryText.isNotBlank()) {
                                    viewModel.searchAddress(searchQueryText)
                                } else {
                                    viewModel.resetToGpsLocation()
                                    fetchDeviceLocation(context) { lat, lng ->
                                        viewModel.updateLocationFromGps(lat, lng)
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = "Buscar o restablecer mi GPS",
                                    tint = Turquoise
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = viewMode == 0,
                                onClick = { viewMode = 0 },
                                label = { Text("🗺️ Mapa") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = viewMode == 1,
                                onClick = { viewMode = 1 },
                                label = { Text("📋 Lista (${currentVets.size})") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (!hasLocationPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Escribe tu ciudad o dirección arriba y presiona buscar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedVet != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                selectedVet?.let { vet ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    vet.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.selectVet(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (vet.isOpen) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (vet.isOpen) "Abierto" else "Cerrado",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "⭐ ${vet.rating} • ${vet.distanceKm} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Turquoise,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    vet.address,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Turquoise,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    vet.phone,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vet.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Llamar", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }

                                Button(
                                    onClick = {
                                        viewModel.toggleRoute(true)
                                        try {
                                            val gmmIntentUri = Uri.parse("google.navigation:q=${vet.latitude},${vet.longitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${vet.latitude},${vet.longitude}"))
                                            context.startActivity(webIntent)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise)
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cómo llegar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        var lastLocation: Location? = null
        if (isGpsEnabled) {
            lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        if (lastLocation == null && isNetworkEnabled) {
            lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (lastLocation != null) {
            onLocationReceived(lastLocation.latitude, lastLocation.longitude)
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationReceived(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
                @Deprecated("Deprecated in API 29")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            locationManager.requestLocationUpdates(provider, 1000L, 10f, listener)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onLocationReceived(4.6097, -74.0817)
    }
}

private fun updateMapData(
    webView: WebView?,
    lat: Double,
    lng: Double,
    vets: List<Vet>,
    selectedVet: Vet?,
    showRoute: Boolean
) {
    if (webView == null) return
    val vetsJson = JSONArray()
    vets.forEach { vet ->
        val item = JSONObject().apply {
            put("id", vet.id)
            put("name", vet.name)
            put("address", vet.address)
            put("phone", vet.phone)
            put("lat", vet.latitude)
            put("lng", vet.longitude)
            put("distanceKm", vet.distanceKm)
            put("isOpen", vet.isOpen)
        }
        vetsJson.put(item)
    }

    val selectedVetId = selectedVet?.id ?: ""
    val targetVetLat = selectedVet?.latitude ?: 0.0
    val targetVetLng = selectedVet?.longitude ?: 0.0

    val script = "javascript:if(window.updateMap){ window.updateMap($lat, $lng, $vetsJson, '$selectedVetId', $showRoute, $targetVetLat, $targetVetLng); }"
    webView.evaluateJavascript(script, null)
}

private fun getMapHtmlContent(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <style>
                html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; background: #e5e3df; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script>
                var map = null;
                var userMarker = null;
                var radiusCircle = null;
                var vetMarkers = [];
                var routePolyline = null;
                var pendingMapData = null;

                function initMap() {
                    if (map || typeof L === 'undefined') return;

                    map = L.map('map', { zoomControl: false }).setView([4.6097, -74.0817], 14);

                    L.tileLayer('https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                        maxZoom: 19,
                        subdomains: 'abcd',
                        attribution: '© OpenStreetMap © CARTO'
                    }).addTo(map);

                    setTimeout(function() {
                        map.invalidateSize();
                    }, 300);

                    if (pendingMapData) {
                        window.updateMap.apply(null, pendingMapData);
                    }
                }

                window.updateMap = function(userLat, userLng, vetsList, selectedVetId, showRoute, targetLat, targetLng) {
                    if (!map || typeof L === 'undefined') {
                        pendingMapData = [userLat, userLng, vetsList, selectedVetId, showRoute, targetLat, targetLng];
                        initMap();
                        return;
                    }

                    map.invalidateSize();
                    var userPos = [userLat, userLng];

                    map.setView(userPos, 14);

                    if (userMarker) map.removeLayer(userMarker);
                    userMarker = L.circleMarker(userPos, {
                        radius: 10,
                        color: '#FFFFFF',
                        weight: 3,
                        fillColor: '#1A73E8',
                        fillOpacity: 1
                    }).addTo(map).bindPopup("<b>Tu Ubicación</b>");

                    if (radiusCircle) map.removeLayer(radiusCircle);
                    radiusCircle = L.circle(userPos, {
                        radius: 5000,
                        color: '#1A73E8',
                        weight: 2,
                        dashArray: '5, 10',
                        fillColor: '#1A73E8',
                        fillOpacity: 0.08
                    }).addTo(map);

                    vetMarkers.forEach(function(m) { map.removeLayer(m); });
                    vetMarkers = [];

                    if (routePolyline) {
                        map.removeLayer(routePolyline);
                        routePolyline = null;
                    }

                    vetsList.forEach(function(vet) {
                        var vetPos = [vet.lat, vet.lng];
                        var isSelected = (vet.id === selectedVetId);

                        var markerColor = isSelected ? '#D93025' : '#EA4335';
                        var marker = L.circleMarker(vetPos, {
                            radius: isSelected ? 14 : 10,
                            color: '#FFFFFF',
                            weight: 2,
                            fillColor: markerColor,
                            fillOpacity: 1
                        }).addTo(map);

                        marker.on('click', function() {
                            if (window.AndroidMap) {
                                window.AndroidMap.onVetSelected(vet.id);
                            }
                        });

                        vetMarkers.push(marker);
                    });

                    if (showRoute && targetLat !== 0 && targetLng !== 0) {
                        var routePoints = [userPos, [targetLat, targetLng]];
                        routePolyline = L.polyline(routePoints, {
                            color: '#D93025',
                            weight: 5,
                            opacity: 0.9,
                            dashArray: '10, 10'
                        }).addTo(map);

                        map.fitBounds(L.latLngBounds(routePoints), { padding: [60, 60] });
                    }
                };

                if (document.readyState === 'complete') {
                    initMap();
                } else {
                    window.addEventListener('load', initMap);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
