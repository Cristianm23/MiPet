package com.example.mipet.data.repository

import com.example.mipet.data.model.Resource
import com.example.mipet.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, pass: String): Resource<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun register(name: String, email: String, pass: String): Resource<Boolean> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("UID nulo")
            val user = User(uid, name, email)
            firestore.collection("usuarios").document(uid).set(user).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al registrar")
        }
    }

    fun logout() = auth.signOut()

    fun getCurrentUser() = auth.currentUser
}
