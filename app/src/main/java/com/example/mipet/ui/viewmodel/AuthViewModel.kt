package com.example.mipet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.Resource
import com.example.mipet.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _authState = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
    val authState = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            _authState.value = repository.login(email, pass)
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            _authState.value = repository.register(name, email, pass)
        }
    }

    fun logout() = repository.logout()

    fun isUserLoggedIn() = repository.getCurrentUser() != null
}
