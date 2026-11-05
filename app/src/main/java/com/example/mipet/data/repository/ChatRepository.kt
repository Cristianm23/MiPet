package com.example.mipet.data.repository

import com.example.mipet.data.model.ChatMessage
import com.example.mipet.data.model.Resource
import com.example.mipet.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    suspend fun getRegisteredUsers(): Resource<List<User>> {
        return try {
            val currentUid = getCurrentUserId()
            val snapshot = firestore.collection("usuarios").get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                .filter { it.uid != currentUid }
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener usuarios")
        }
    }

    fun getChatMessages(partnerUid: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUid = getCurrentUserId()
        val chatId = if (currentUid < partnerUid) "${currentUid}_$partnerUid" else "${partnerUid}_$currentUid"

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("mensajes")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(partnerUid: String, text: String, senderName: String): Resource<Boolean> {
        return try {
            val currentUid = getCurrentUserId()
            val chatId = if (currentUid < partnerUid) "${currentUid}_$partnerUid" else "${partnerUid}_$currentUid"
            
            val messageRef = firestore.collection("chats").document(chatId).collection("mensajes").document()
            val message = ChatMessage(
                id = messageRef.id,
                senderId = currentUid,
                senderName = senderName,
                recipientId = partnerUid,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            messageRef.set(message).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al enviar mensaje")
        }
    }
}
