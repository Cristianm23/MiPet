package com.example.mipet.data.model

data class Pet(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val gender: String = "",
    val birthDate: Long = 0L,
    val weight: Double = 0.0,
    val photoUrls: List<String> = emptyList(),
    val registrationDate: Long = System.currentTimeMillis()
)
