package com.example.mipet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mipet.ui.navigation.Screen
import com.example.mipet.ui.theme.Turquoise

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                val items = listOf(
                    Triple("Inicio", Icons.Default.Home, Screen.Home.route),
                    Triple("Mascotas", Icons.Default.Pets, Screen.PetList.route),
                    Triple("Asistente", Icons.AutoMirrored.Filled.Chat, Screen.AiAssistant.route)
                )
                
                items.forEach { (label, icon, route) ->
                    NavigationBarItem(
                        selected = route == Screen.Home.route,
                        onClick = { if (route != Screen.Home.route) navController.navigate(route) },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Hola de nuevo,",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Bienvenido a MiPet",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = Turquoise)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Resumen del día",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No hay actividades pendientes para hoy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
