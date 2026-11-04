package com.example.mipet.data.model

data class Vet(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double = 0.0,
    val isOpen: Boolean = true,
    val rating: Double = 4.8,
    val openHours: String = "24 horas / Abierto"
)
