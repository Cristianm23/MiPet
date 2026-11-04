package com.example.mipet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.Resource
import com.example.mipet.data.model.Vet
import com.example.mipet.data.repository.VetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VetRepository(application.applicationContext)

    private val _vetsState = MutableStateFlow<Resource<List<Vet>>>(Resource.Loading())
    val vetsState = _vetsState.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _locationLabel = MutableStateFlow("Tu Ubicación")
    val locationLabel = _locationLabel.asStateFlow()

    private val _isCustomSearch = MutableStateFlow(false)
    val isCustomSearch = _isCustomSearch.asStateFlow()

    private val _selectedVet = MutableStateFlow<Vet?>(null)
    val selectedVet = _selectedVet.asStateFlow()

    private val _showRoute = MutableStateFlow(false)
    val showRoute = _showRoute.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateLocationFromGps(lat: Double, lng: Double) {
        if (!_isCustomSearch.value) {
            _userLocation.value = Pair(lat, lng)
            _locationLabel.value = "Tu Ubicación GPS"
            loadNearbyVets(lat, lng, cityName = "")
        }
    }

    fun loadNearbyVets(lat: Double, lng: Double, cityName: String = "") {
        viewModelScope.launch {
            _vetsState.value = Resource.Loading()
            try {
                val vets = repository.getNearbyVets(lat, lng, cityName = cityName, radiusKm = 5.0)
                _vetsState.value = Resource.Success(vets)
            } catch (e: Exception) {
                _vetsState.value = Resource.Error("Error al cargar veterinarias cercanas.")
            }
        }
    }

    fun searchAddress(query: String) {
        if (query.isBlank()) return
        _searchQuery.value = query
        _isCustomSearch.value = true
        viewModelScope.launch {
            _vetsState.value = Resource.Loading()
            val result = repository.geocodeAddress(query)
            if (result != null) {
                _userLocation.value = Pair(result.latitude, result.longitude)
                _locationLabel.value = result.displayName
                loadNearbyVets(result.latitude, result.longitude, cityName = result.displayName)
            } else {
                _vetsState.value = Resource.Error("No se encontró la ubicación de '$query'.")
            }
        }
    }

    fun resetToGpsLocation() {
        _isCustomSearch.value = false
        _searchQuery.value = ""
        _locationLabel.value = "Tu Ubicación GPS"
    }

    fun selectVet(vet: Vet?) {
        _selectedVet.value = vet
        _showRoute.value = false
    }

    fun toggleRoute(show: Boolean) {
        _showRoute.value = show
    }
}
