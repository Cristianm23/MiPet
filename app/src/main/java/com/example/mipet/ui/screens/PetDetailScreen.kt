package com.example.mipet.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.mipet.data.model.HealthControl
import com.example.mipet.data.model.Pet
import com.example.mipet.data.model.Resource
import com.example.mipet.ui.navigation.Screen
import com.example.mipet.ui.theme.PetroleumBlue
import com.example.mipet.ui.theme.Turquoise
import com.example.mipet.ui.viewmodel.HealthViewModel
import com.example.mipet.ui.viewmodel.PetViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    navController: NavController,
    petId: String,
    petViewModel: PetViewModel = viewModel(),
    healthViewModel: HealthViewModel = viewModel()
) {
    val petState by petViewModel.selectedPet.collectAsState()
    val healthState by healthViewModel.controls.collectAsState()

    LaunchedEffect(petId) {
        petViewModel.getPetById(petId)
        healthViewModel.loadControls(petId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha Médica") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Edit Pet */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = petState) {
            is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is Resource.Error -> Text("Error: ${state.message}")
            is Resource.Success -> {
                val pet = state.data!!
                DetailContent(padding, pet, healthState, navController)
            }
        }
    }
}

@Composable
fun DetailContent(
    padding: PaddingValues,
    pet: Pet,
    healthState: Resource<List<HealthControl>>,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        // Pet Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (pet.photoUrls.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(pet.photoUrls.first()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = PetroleumBlue, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(pet.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${pet.species} • ${pet.breed}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }

        if (pet.photoUrls.size > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pet.photoUrls.drop(1)) { url ->
                    Image(
                        painter = rememberAsyncImagePainter(url),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Health Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Historial de Salud", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = { navController.navigate(Screen.AddHealthControl.createRoute(pet.id)) },
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Health History List
        when (val state = healthState) {
            is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is Resource.Error -> Text("Error al cargar historial")
            is Resource.Success -> {
                val controls = state.data?.sortedByDescending { it.date } ?: emptyList()
                if (controls.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay registros médicos aún.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(controls) { control ->
                            HealthCard(control)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthCard(control: HealthControl) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val icon: ImageVector
    val iconColor: Color

    when (control.type) {
        "Vacuna" -> {
            icon = Icons.Default.Vaccines
            iconColor = PetroleumBlue
        }
        "Desparasitación" -> {
            icon = Icons.Default.BugReport
            iconColor = Turquoise
        }
        else -> {
            icon = Icons.Default.MedicalServices
            iconColor = MaterialTheme.colorScheme.secondary
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(control.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(control.type, style = MaterialTheme.typography.bodySmall, color = iconColor)
                if (control.observations.isNotBlank()) {
                    Text(
                        control.observations,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
            }
            Text(
                sdf.format(Date(control.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
