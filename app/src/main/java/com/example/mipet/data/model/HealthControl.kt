package com.example.mipet.data.model

data class HealthControl(
    val id: String = "",
    val petId: String = "",
    val type: String = "",
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val observations: String = ""
)
