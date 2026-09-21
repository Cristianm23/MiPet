package com.example.mipet.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.Pet
import com.example.mipet.data.model.Resource
import com.example.mipet.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PetViewModel(private val repository: PetRepository = PetRepository()) : ViewModel() {
    private val _pets = MutableStateFlow<Resource<List<Pet>>>(Resource.Loading())
    val pets: StateFlow<Resource<List<Pet>>> = _pets.asStateFlow()

    private val _addPetStatus = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
    val addPetStatus: StateFlow<Resource<Boolean>> = _addPetStatus.asStateFlow()

    private val _selectedPet = MutableStateFlow<Resource<Pet>>(Resource.Loading())
    val selectedPet: StateFlow<Resource<Pet>> = _selectedPet.asStateFlow()

    init {
        loadPets()
    }

    fun getPetById(petId: String) {
        viewModelScope.launch {
            _selectedPet.value = Resource.Loading()
            val result = repository.getPetById(petId)
            _selectedPet.value = result
        }
    }

    private fun loadPets() {
        viewModelScope.launch {
            repository.getPets().collect { _pets.value = it }
        }
    }

    fun addPet(pet: Pet, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _addPetStatus.value = Resource.Loading()
            val result = repository.addPet(pet, imageUris)
            _addPetStatus.value = result
        }
    }

    fun deletePet(petId: String) {
        viewModelScope.launch {
            repository.deletePet(petId)
        }
    }
}
