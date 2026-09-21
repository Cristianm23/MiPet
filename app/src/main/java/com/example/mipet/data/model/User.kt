package com.example.mipet.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val registrationDate: Long = System.currentTimeMillis()
)
