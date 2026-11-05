package com.example.mipet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.Resource
import com.example.mipet.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _name = MutableStateFlow("Dueño de Mascota")
    val name = _name.asStateFlow()

    private val _description = MutableStateFlow("Amante de los animales y feliz dueño de mascotas en MiPet 🐾")
    val description = _description.asStateFlow()

    private val _photoUri = MutableStateFlow<String?>(null)
    val photoUri = _photoUri.asStateFlow()

    private val _saveStatus = MutableStateFlow<Resource<Boolean>?>(null)
    val saveStatus = _saveStatus.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            viewModelScope.launch {
                when (val result = authRepository.getUserProfile(currentUser.uid)) {
                    is Resource.Success -> {
                        result.data?.let { user ->
                            if (user.name.isNotBlank()) _name.value = user.name
                            if (user.description.isNotBlank()) _description.value = user.description
                            if (user.photoUrl.isNotBlank()) _photoUri.value = user.photoUrl
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateProfile(newName: String, newDescription: String, newPhotoUri: String? = null) {
        val currentUser = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            _saveStatus.value = Resource.Loading()
            val photoToSave = newPhotoUri ?: _photoUri.value ?: ""
            val result = authRepository.updateUserProfile(currentUser.uid, newName, newDescription, photoToSave)
            if (result is Resource.Success) {
                _name.value = newName
                _description.value = newDescription
                if (newPhotoUri != null) {
                    _photoUri.value = newPhotoUri
                }
            }
            _saveStatus.value = result
        }
    }

    fun updatePhotoUri(uri: String?) {
        _photoUri.value = uri
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null && uri != null) {
            updateProfile(_name.value, _description.value, uri)
        }
    }
}
