package com.example.mipet.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
