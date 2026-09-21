package com.example.mipet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.Resource
import com.example.mipet.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiViewModel(private val repository: AiRepository = AiRepository()) : ViewModel() {
    private val _aiResponse = MutableStateFlow<Resource<String>>(Resource.Success(""))
    val aiResponse = _aiResponse.asStateFlow()

    fun getRecommendation(prompt: String) {
        viewModelScope.launch {
            _aiResponse.value = Resource.Loading()
            _aiResponse.value = repository.getRecommendation(prompt)
        }
    }
}
