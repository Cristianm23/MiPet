package com.example.mipet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mipet.data.model.HealthControl
import com.example.mipet.data.model.Resource
import com.example.mipet.ui.viewmodel.HealthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHealthControlScreen(
    navController: NavController,
    petId: String,
    viewModel: HealthViewModel = viewModel()
) {
    var type by remember { mutableStateOf("Vacuna") }
    var description by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val addStatus by viewModel.addControlStatus.collectAsState()
    val types = listOf("Vacuna", "Desparasitación", "Chequeo General", "Otro")
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(addStatus) {
        if (addStatus is Resource.Success && (addStatus as Resource.Success<Boolean>).data == true) {
            viewModel.resetAddStatus()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Control de Salud") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Registrar Cuidado Médico",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Type Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Control") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    types.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                type = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Nombre / Descripción (Ej: Sextuple, Pipeta)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple Date Display (could be improved with DatePicker)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            OutlinedTextField(
                value = sdf.format(Date(date)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = observations,
                onValueChange = { observations = it },
                label = { Text("Observaciones del Veterinario") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = MaterialTheme.shapes.medium,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val control = HealthControl(
                        petId = petId,
                        type = type,
                        description = description,
                        observations = observations,
                        date = date
                    )
                    viewModel.addControl(control)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = description.isNotBlank() && addStatus !is Resource.Loading
            ) {
                if (addStatus is Resource.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Guardar Historial", fontWeight = FontWeight.Bold)
                }
            }
            
            if (addStatus is Resource.Error) {
                Text(
                    text = (addStatus as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun rememberStateLongOf(initialValue: Long): MutableState<Long> = remember { mutableLongStateOf(initialValue) }
