package com.example.mipet.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mipet.data.model.Resource
import com.example.mipet.data.model.Vet
import com.example.mipet.ui.navigation.Screen
import com.example.mipet.ui.theme.Turquoise
import com.example.mipet.ui.viewmodel.VetViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*

@Composable
fun VetMapScreen(navController: NavController, viewModel: VetViewModel = viewModel()) {
    val context = LocalContext.current

    val vetsState by viewModel.vetsState.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val isCustomSearch by viewModel.isCustomSearch.collectAsState()
    val selectedVet by viewModel.selectedVet.collectAsState()

    var searchQueryText by remember { mutableStateOf("") }
    var viewMode by remember { mutableIntStateOf(0) }
    var locationErrorMessage by remember { mutableStateOf<String?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }

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

    val cameraPositionState = rememberCameraPositionState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            locationErrorMessage = null
            isFetchingLocation = true
            fetchDeviceLocation(
                context = context,
                onLocationReceived = { lat, lng ->
                    isFetchingLocation = false
                    viewModel.updateLocationFromGps(lat, lng)
                },
                onLocationFailed = { error ->
                    isFetchingLocation = false
                    locationErrorMessage = error
                }
            )
        } else {
            isFetchingLocation = false
            locationErrorMessage = "Se requieren permisos de ubicación para detectar tu posición."
        }
    }

    fun requestLocationFetch() {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            isFetchingLocation = true
            locationErrorMessage = null
            fetchDeviceLocation(
                context = context,
                onLocationReceived = { lat, lng ->
                    isFetchingLocation = false
                    viewModel.updateLocationFromGps(lat, lng)
                },
                onLocationFailed = { error ->
                    isFetchingLocation = false
                    locationErrorMessage = error
                }
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && userLocation == null && !isCustomSearch) {
            requestLocationFetch()
        }
    }

    LaunchedEffect(selectedVet) {
        selectedVet?.let { vet ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(vet.latitude, vet.longitude),
                    15f
                )
            )
        }
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { (lat, lng) ->
            if (selectedVet == null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(lat, lng),
                        14f
                    )
                )
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
            val currentVets = (vetsState as? Resource.Success)?.data ?: emptyList()

            if (viewMode == 0) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = hasLocationPermission
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            compassEnabled = true
                        )
                    ) {
                        userLocation?.let { (lat, lng) ->
                            val userMarkerState = rememberMarkerState(key = "user_${lat}_${lng}", position = LatLng(lat, lng))
                            Marker(
                                state = userMarkerState,
                                title = "Tu Ubicación",
                                snippet = locationLabel,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }

                        currentVets.forEach { vet ->
                            val isSelected = vet.id == selectedVet?.id
                            val vetMarkerState = rememberMarkerState(key = "vet_${vet.id}", position = LatLng(vet.latitude, vet.longitude))
                            Marker(
                                state = vetMarkerState,
                                title = vet.name,
                                snippet = "${vet.address} • ${vet.distanceKm} km",
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    if (isSelected) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_ROSE
                                ),
                                onClick = {
                                    viewModel.selectVet(vet)
                                    true
                                }
                            )
                        }
                    }

                    if (isFetchingLocation || vetsState is Resource.Loading) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(
                                    text = if (isFetchingLocation) "Obteniendo ubicación del dispositivo..." else "Buscando veterinarias cercanas...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
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

                    if (vetsState is Resource.Loading || isFetchingLocation) {
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

                                val hasPhone = vet.phone.isNotBlank() &&
                                        vet.phone != "Teléfono no registrado"
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
                                        if (hasPhone) {
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
                                    requestLocationFetch()
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable { requestLocationFetch() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Permiso de ubicación desactivado. Toca aquí para concederlo o busca una ciudad arriba.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else if (locationErrorMessage != null && userLocation == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable { requestLocationFetch() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                locationErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Reintentar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Turquoise
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

                    val hasPhone = vet.phone.isNotBlank() &&
                            vet.phone != "Teléfono no registrado"

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
                                Text(
                                    "${vet.distanceKm} km de distancia",
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
                                if (hasPhone) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${vet.phone}")
                                            )
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Llamar",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.toggleRoute(true)
                                        try {
                                            val gmmIntentUri = Uri.parse("google.navigation:q=${vet.latitude},${vet.longitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            context.startActivity(mapIntent)
                                        } catch (_: Exception) {
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
private fun fetchDeviceLocation(
    context: Context,
    onLocationReceived: (Double, Double) -> Unit,
    onLocationFailed: (String) -> Unit
) {
    try {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isLocationEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isLocationEnabled) {
            onLocationFailed(
                "Los servicios de ubicación están desactivados. Activa el GPS e intenta nuevamente."
            )
            return
        }

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->

            if (location == null) {
                onLocationFailed(
                    "No se pudo obtener tu ubicación actual. Comprueba tu conexión y vuelve a intentarlo."
                )
                return@addOnSuccessListener
            }

            // Comprobar que las coordenadas sean válidas.
            if (
                location.latitude !in -90.0..90.0 ||
                location.longitude !in -180.0..180.0
            ) {
                onLocationFailed("El dispositivo devolvió coordenadas inválidas.")
                return@addOnSuccessListener
            }

            // Comprobar que la ubicación sea reciente.
            val locationAgeNanos =
                android.os.SystemClock.elapsedRealtimeNanos() -
                        location.elapsedRealtimeNanos

            val maxAgeNanos = 2 * 60 * 1_000_000_000L

            if (locationAgeNanos < 0 || locationAgeNanos > maxAgeNanos) {
                onLocationFailed(
                    "La ubicación recibida está desactualizada. Intenta nuevamente."
                )
                return@addOnSuccessListener
            }

            // Comprobar que exista información de precisión.
            if (!location.hasAccuracy() || location.accuracy > 1000f) {
                onLocationFailed(
                    "La ubicación no tiene suficiente precisión. Intenta nuevamente en un lugar abierto."
                )
                return@addOnSuccessListener
            }

            onLocationReceived(
                location.latitude,
                location.longitude
            )

        }.addOnFailureListener { exception ->
            onLocationFailed(
                "Error al obtener tu ubicación: ${
                    exception.localizedMessage ?: "error desconocido"
                }"
            )
        }

    } catch (exception: Exception) {
        onLocationFailed(
            "Error al acceder al servicio de ubicación: ${
                exception.localizedMessage ?: "error desconocido"
            }"
        )
    }
}

