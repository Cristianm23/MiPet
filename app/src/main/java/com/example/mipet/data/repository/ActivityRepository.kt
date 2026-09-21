package com.example.mipet.data.repository

import com.example.mipet.data.model.Activity
import com.example.mipet.data.model.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ActivityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addActivity(activity: Activity): Resource<Boolean> {
        return try {
            val docRef = firestore.collection("actividades").document()
            val newActivity = activity.copy(id = docRef.id)
            docRef.set(newActivity).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al agregar actividad")
        }
    }

    fun getActivities(petId: String): Flow<Resource<List<Activity>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("actividades")
            .whereEqualTo("petId", petId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Resource.Error(e.message ?: "Error de red"))
                    return@addSnapshotListener
                }
                val activities = snapshot?.toObjects(Activity::class.java) ?: emptyList()
                trySend(Resource.Success(activities))
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateActivity(activity: Activity): Resource<Boolean> {
        return try {
            firestore.collection("actividades").document(activity.id).set(activity).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al actualizar")
        }
    }

    suspend fun deleteActivity(activityId: String): Resource<Boolean> {
        return try {
            firestore.collection("actividades").document(activityId).delete().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar")
        }
    }
}
