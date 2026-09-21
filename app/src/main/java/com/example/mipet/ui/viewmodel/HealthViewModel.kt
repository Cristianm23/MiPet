package com.example.mipet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.HealthControl
import com.example.mipet.data.model.Resource
import com.example.mipet.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthViewModel(private val repository: HealthRepository = HealthRepository()) : ViewModel() {
    private val _controls = MutableStateFlow<Resource<List<HealthControl>>>(Resource.Loading())
    val controls: StateFlow<Resource<List<HealthControl>>> = _controls.asStateFlow()

    private val _addControlStatus = MutableStateFlow<Resource<Boolean>>(Resource.Success(false))
    val addControlStatus: StateFlow<Resource<Boolean>> = _addControlStatus.asStateFlow()

    fun loadControls(petId: String) {
        viewModelScope.launch {
            repository.getControls(petId).collect {
                _controls.value = it
            }
        }
    }

    fun addControl(control: HealthControl) {
        viewModelScope.launch {
            _addControlStatus.value = Resource.Loading()
            val result = repository.addControl(control)
            _addControlStatus.value = result
        }
    }

    fun resetAddStatus() {
        _addControlStatus.value = Resource.Success(false)
    }
}
