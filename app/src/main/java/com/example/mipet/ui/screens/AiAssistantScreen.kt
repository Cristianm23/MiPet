package com.example.mipet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mipet.data.model.Resource
import com.example.mipet.ui.components.VoiceAgentComponent
import com.example.mipet.ui.navigation.Screen
import com.example.mipet.ui.theme.Turquoise
import com.example.mipet.ui.viewmodel.AiViewModel

@Composable
fun AiAssistantScreen(navController: NavController, viewModel: AiViewModel = viewModel()) {
    var prompt by remember { mutableStateOf("") }
    val aiResponse by viewModel.aiResponse.collectAsState()

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
                    Triple("Asistente", Icons.AutoMirrored.Filled.Chat, Screen.AiAssistant.route),
                    Triple("Perfil", Icons.Default.Person, Screen.Profile.route)
                )

                items.forEach { (label, icon, route) ->
                    NavigationBarItem(
                        selected = route == Screen.AiAssistant.route,
                        onClick = { if (route != Screen.AiAssistant.route) navController.navigate(route) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Asistente IA 🐾", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Pregunta sobre el cuidado de tu mascota:")
            TextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Consulta") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.getRecommendation(prompt) }) {
                Text("Consultar")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = aiResponse) {
                is Resource.Loading -> CircularProgressIndicator()
                is Resource.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                is Resource.Success -> {
                    val data = state.data
                    if (data != null && (data.text.isNotEmpty() || data.voiceAgent)) {
                        if (data.text.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = data.text,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        if (data.voiceAgent) {
                            Spacer(modifier = Modifier.height(16.dp))
                            VoiceAgentComponent(
                                connection = data.voiceCallConfig,
                                configurationError = data.voiceCallDiagnostic,
                                onCloseCall = { viewModel.resetState() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Nota: Esta IA es solo informativa, no sustituye al veterinario.",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
