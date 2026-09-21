package com.example.mipet.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object PetList : Screen("pet_list")
    object PetDetail : Screen("pet_detail/{petId}") {
        fun createRoute(petId: String) = "pet_detail/$petId"
    }
    object AiAssistant : Screen("ai_assistant")
    object Profile : Screen("profile")
    object AddPet : Screen("add_pet")
    object AddHealthControl : Screen("add_health_control/{petId}") {
        fun createRoute(petId: String) = "add_health_control/$petId"
    }
}
