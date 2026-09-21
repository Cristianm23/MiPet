package com.example.mipet.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.mipet.data.model.Pet
import com.example.mipet.data.model.Resource
import com.example.mipet.ui.viewmodel.PetViewModel
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(navController: NavController, viewModel: PetViewModel = viewModel()) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Macho") }
    var weight by remember { mutableStateOf("") }
    val selectedImages = remember { mutableStateListOf<Uri>() }
    
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val addPetStatus by viewModel.addPetStatus.collectAsState()

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            if (selectedImages.size < 5) {
                selectedImages.add(tempCameraUri!!)
            }
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val remaining = 5 - selectedImages.size
        selectedImages.addAll(uris.take(remaining))
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera(context, { tempCameraUri = it }, { cameraLauncher.launch(it) })
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(addPetStatus) {
        if (addPetStatus is Resource.Success && (addPetStatus as Resource.Success<Boolean>).data == true) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Mascota") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo List
            Text("Fotos de tu mascota (${selectedImages.size}/5)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImages) { uri ->
                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImages.remove(uri) },
                            modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape).size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                if (selectedImages.size < 5) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showImageSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Especie (Ej: Perro, Gato)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                label = { Text("Raza") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Género:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = gender == "Macho", onClick = { gender = "Macho" })
                Text("Macho")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(selected = gender == "Hembra", onClick = { gender = "Hembra" })
                Text("Hembra")
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Peso (kg)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val pet = Pet(
                        name = name,
                        species = species,
                        breed = breed,
                        gender = gender,
                        weight = weight.toDoubleOrNull() ?: 0.0
                    )
                    viewModel.addPet(pet, selectedImages.toList())
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = name.isNotBlank() && addPetStatus !is Resource.Loading
            ) {
                if (addPetStatus is Resource.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Guardar Mascota", fontWeight = FontWeight.Bold)
                }
            }
            
            if (addPetStatus is Resource.Error) {
                Text(
                    text = (addPetStatus as Resource.Error).message ?: "Error al guardar",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Seleccionar origen") },
            text = { Text("¿Desde dónde quieres añadir la foto?") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galería")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                            launchCamera(context, { tempCameraUri = it }, { cameraLauncher.launch(it) })
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cámara")
                }
            }
        )
    }
}

private fun launchCamera(context: Context, onUriCreated: (Uri) -> Unit, onLaunch: (Uri) -> Unit) {
    val photoFile = File.createTempFile(
        "PET_${System.currentTimeMillis()}",
        ".jpg",
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    )
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    onUriCreated(uri)
    onLaunch(uri)
}
