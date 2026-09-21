package com.example.mipet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mipet.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.PetList.route) { PetListScreen(navController) }
        composable(Screen.PetDetail.route) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            PetDetailScreen(navController, petId)
        }
        composable(Screen.AiAssistant.route) { AiAssistantScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.AddPet.route) { AddPetScreen(navController) }
        composable(Screen.AddHealthControl.route) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AddHealthControlScreen(navController, petId)
        }
    }
}
