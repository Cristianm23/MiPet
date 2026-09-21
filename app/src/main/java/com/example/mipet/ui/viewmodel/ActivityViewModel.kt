package com.example.mipet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.Activity
import com.example.mipet.data.model.HealthControl
import com.example.mipet.data.model.Resource
import com.example.mipet.data.repository.ActivityRepository
import com.example.mipet.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityViewModel(
    private val activityRepo: ActivityRepository = ActivityRepository(),
    private val healthRepo: HealthRepository = HealthRepository()
) : ViewModel() {

    private val _activities = MutableStateFlow<Resource<List<Activity>>>(Resource.Loading())
    val activities = _activities.asStateFlow()

    private val _controls = MutableStateFlow<Resource<List<HealthControl>>>(Resource.Loading())
    val controls = _controls.asStateFlow()

    fun loadData(petId: String) {
        viewModelScope.launch {
            activityRepo.getActivities(petId).collect { _activities.value = it }
        }
        viewModelScope.launch {
            healthRepo.getControls(petId).collect { _controls.value = it }
        }
    }

    fun addActivity(activity: Activity) = viewModelScope.launch { activityRepo.addActivity(activity) }
    fun completeActivity(activity: Activity) = viewModelScope.launch { 
        activityRepo.updateActivity(activity.copy(completed = true)) 
    }
    fun addControl(control: HealthControl) = viewModelScope.launch { healthRepo.addControl(control) }
}
