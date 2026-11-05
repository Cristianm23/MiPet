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

    suspend fun getUserProfile(uid: String): Resource<User> {
        return try {
            val doc = firestore.collection("usuarios").document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: User(uid = uid)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al cargar perfil")
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, description: String, photoUrl: String): Resource<Boolean> {
        return try {
            val updates = mapOf(
                "name" to name,
                "description" to description,
                "photoUrl" to photoUrl
            )
            firestore.collection("usuarios").document(uid).update(updates).await()
            Resource.Success(true)
        } catch (e: Exception) {
            try {
                val user = User(uid = uid, name = name, description = description, photoUrl = photoUrl)
                firestore.collection("usuarios").document(uid).set(user).await()
                Resource.Success(true)
            } catch (ex: Exception) {
                Resource.Error(ex.message ?: "Error al actualizar perfil")
            }
        }
    }

    fun logout() = auth.signOut()

    fun getCurrentUser() = auth.currentUser
}
