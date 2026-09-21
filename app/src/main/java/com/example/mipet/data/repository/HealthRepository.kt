package com.example.mipet.data.repository

import com.example.mipet.data.model.HealthControl
import com.example.mipet.data.model.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HealthRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addControl(control: HealthControl): Resource<Boolean> {
        return try {
            val docRef = firestore.collection("controles").document()
            val newControl = control.copy(id = docRef.id)
            docRef.set(newControl).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al agregar control")
        }
    }

    fun getControls(petId: String): Flow<Resource<List<HealthControl>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("controles")
            .whereEqualTo("petId", petId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Resource.Error(e.message ?: "Error de red"))
                    return@addSnapshotListener
                }
                val controls = snapshot?.toObjects(HealthControl::class.java) ?: emptyList()
                trySend(Resource.Success(controls))
            }
        awaitClose { listener.remove() }
    }
}
