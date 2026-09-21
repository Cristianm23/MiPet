package com.example.mipet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mipet.data.model.Resource
import com.example.mipet.ui.viewmodel.AiViewModel

@Composable
fun AiAssistantScreen(navController: NavController, viewModel: AiViewModel = viewModel()) {
    var prompt by remember { mutableStateOf("") }
    val aiResponse by viewModel.aiResponse.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                if (state.data?.isNotEmpty() == true) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.data,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
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
