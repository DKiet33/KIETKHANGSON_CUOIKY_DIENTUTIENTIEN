package com.khangkietson.smarthome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.khangkietson.smarthome.data.model.Device
import com.khangkietson.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(private val repository: DeviceRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.devices
        .map { devices -> HomeUiState(devices = devices) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun toggleDevice(id: String) {
        repository.toggleDevice(id)
    }

    fun setBrightness(id: String, brightness: Int) {
        repository.setBrightness(id, brightness)
    }

    fun setFanSpeed(id: String, speed: Int) {
        repository.setFanSpeed(id, speed)
    }

    fun toggleDoor(id: String) {
        repository.toggleDoor(id)
    }

    companion object {
        fun provideFactory(repository: DeviceRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
