package com.example.mipet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.Resource
import com.example.mipet.data.model.Vet
import com.example.mipet.data.repository.VetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VetRepository(application.applicationContext)
    private var nearbyVetsJob: Job? = null
    private var geocodingJob: Job? = null

    private val _vetsState = MutableStateFlow<Resource<List<Vet>>>(Resource.Loading())
    val vetsState = _vetsState.asStateFlow()
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation = _userLocation.asStateFlow()
    private val _locationLabel = MutableStateFlow("Busca una ciudad o usa tu ubicación")
    val locationLabel = _locationLabel.asStateFlow()
    private val _isCustomSearch = MutableStateFlow(false)
    val isCustomSearch = _isCustomSearch.asStateFlow()
    private val _selectedVet = MutableStateFlow<Vet?>(null)
    val selectedVet = _selectedVet.asStateFlow()
    private val _showRoute = MutableStateFlow(false)
    val showRoute = _showRoute.asStateFlow()

    fun updateLocationFromGps(lat: Double, lng: Double) {
        if (_isCustomSearch.value) return
        _userLocation.value = lat to lng
        _locationLabel.value = "Tu ubicación"
        loadNearbyVets(lat, lng)
    }

    fun loadNearbyVets(lat: Double, lng: Double) {
        nearbyVetsJob?.cancel()
        nearbyVetsJob = viewModelScope.launch {
            _vetsState.value = Resource.Loading()
            try {
                _vetsState.value = Resource.Success(repository.getNearbyVets(lat, lng, radiusKm = 8.0))
            } catch (_: Exception) {
                _vetsState.value = Resource.Error("No se pudieron cargar veterinarias cercanas.")
            }
        }
    }

    fun searchAddress(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return
        _isCustomSearch.value = true
        geocodingJob?.cancel()
        nearbyVetsJob?.cancel()
        geocodingJob = viewModelScope.launch {
            _vetsState.value = Resource.Loading()
            val result = repository.geocodeAddress(trimmedQuery)
            if (result == null) {
                _vetsState.value = Resource.Error("No se encontró “${trimmedQuery}”. Intenta con ciudad y país.")
                return@launch
            }
            _userLocation.value = result.latitude to result.longitude
            _locationLabel.value = result.displayName
            loadNearbyVets(result.latitude, result.longitude)
        }
    }

    fun resetToGpsLocation(lat: Double, lng: Double) {
        _isCustomSearch.value = false
        _locationLabel.value = "Tu ubicación"
        _userLocation.value = lat to lng
        loadNearbyVets(lat, lng)
    }

    fun resetToGpsLocation() {
        _isCustomSearch.value = false
        _selectedVet.value = null
        _showRoute.value = false
    }

    fun selectVet(vet: Vet?) {
        _selectedVet.value = vet
        _showRoute.value = false
    }

    fun toggleRoute(show: Boolean) {
        _showRoute.value = show
    }

    override fun onCleared() {
        nearbyVetsJob?.cancel()
        geocodingJob?.cancel()
        super.onCleared()
    }
}
