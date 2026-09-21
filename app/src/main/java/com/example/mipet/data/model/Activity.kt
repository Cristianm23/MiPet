package com.example.mipet.data.model

data class Activity(
    val id: String = "",
    val petId: String = "",
    val type: String = "",
    val date: Long = System.currentTimeMillis(),
    val time: String = "",
    val description: String = "",
    val completed: Boolean = false
)
