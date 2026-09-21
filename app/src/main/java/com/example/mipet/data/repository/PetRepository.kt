package com.example.mipet.data.repository

import android.net.Uri
import com.example.mipet.data.model.Pet
import com.example.mipet.data.model.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val userId get() = auth.currentUser?.uid ?: ""

    suspend fun addPet(pet: Pet, imageUris: List<Uri> = emptyList()): Resource<Boolean> {
        val currentUserId = userId
        if (currentUserId.isEmpty()) return Resource.Error("Usuario no autenticado")
        
        return try {
            val docRef = firestore.collection("mascotas").document()
            val uploadedUrls = mutableListOf<String>()

            for (index in imageUris.indices) {
                val uri = imageUris[index]
                val storageRef = storage.reference.child("mascotas/${docRef.id}/photo_$index.jpg")
                
                // Upload the file
                storageRef.putFile(uri).await()
                
                // Get the download URL
                val url = storageRef.downloadUrl.await().toString()
                uploadedUrls.add(url)
            }

            val petWithIds = pet.copy(id = docRef.id, userId = currentUserId, photoUrls = uploadedUrls)
            docRef.set(petWithIds).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al agregar mascota")
        }
    }

    fun getPets(): Flow<Resource<List<Pet>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("mascotas")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Resource.Error(e.message ?: "Error de red"))
                    return@addSnapshotListener
                }
                val pets = snapshot?.toObjects(Pet::class.java) ?: emptyList()
                trySend(Resource.Success(pets))
            }
        awaitClose { listener.remove() }
    }

    suspend fun getPetById(petId: String): Resource<Pet> {
        return try {
            val document = firestore.collection("mascotas").document(petId).get().await()
            val pet = document.toObject(Pet::class.java)
            if (pet != null) Resource.Success(pet) else Resource.Error("No encontrado")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al obtener mascota")
        }
    }

    suspend fun updatePet(pet: Pet): Resource<Boolean> {
        return try {
            firestore.collection("mascotas").document(pet.id).set(pet).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al actualizar")
        }
    }

    suspend fun deletePet(petId: String): Resource<Boolean> {
        return try {
            firestore.collection("mascotas").document(petId).delete().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar")
        }
    }
}
