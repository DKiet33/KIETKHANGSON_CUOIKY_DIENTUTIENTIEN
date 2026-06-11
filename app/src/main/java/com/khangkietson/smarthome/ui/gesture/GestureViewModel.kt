package com.khangkietson.smarthome.ui.gesture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.khangkietson.smarthome.data.model.HandGesture
import com.khangkietson.smarthome.data.model.DeviceType
import com.khangkietson.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GestureAction(val description: String) {
    TURN_ON_ALL_LIGHTS("Bật tất cả bóng đèn"),
    TURN_OFF_ALL_LIGHTS("Tắt tất cả bóng đèn"),
    TURN_ON_LIVING_FAN("Bật quạt phòng khách"),
    TURN_OFF_LIVING_FAN("Tắt quạt phòng khách"),
    OPEN_MAIN_DOOR("Mở cửa chính"),
    CLOSE_MAIN_DOOR("Đóng cửa chính"),
    INCREASE_LIVING_FAN_SPEED("Tăng tốc độ quạt phòng khách"),
    NONE("Không làm gì")
}

data class GestureUiState(
    val currentGesture: HandGesture = HandGesture.NONE,
    val confidence: Float = 0f,
    val commandHistory: List<String> = emptyList()
)

class GestureViewModel(private val deviceRepository: DeviceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GestureUiState())
    val uiState: StateFlow<GestureUiState> = _uiState.asStateFlow()

    private val _gestureMappings = MutableStateFlow<Map<HandGesture, GestureAction>>(
        mapOf(
            HandGesture.OPEN_PALM to GestureAction.TURN_ON_ALL_LIGHTS,
            HandGesture.CLOSED_FIST to GestureAction.TURN_OFF_ALL_LIGHTS,
            HandGesture.THUMB_UP to GestureAction.TURN_ON_LIVING_FAN,
            HandGesture.THUMB_DOWN to GestureAction.TURN_OFF_LIVING_FAN,
            HandGesture.VICTORY to GestureAction.OPEN_MAIN_DOOR,
            HandGesture.POINTING_UP to GestureAction.INCREASE_LIVING_FAN_SPEED,
            HandGesture.TWO_FINGERS_CLOSED to GestureAction.CLOSE_MAIN_DOOR
        )
    )
    val gestureMappings: StateFlow<Map<HandGesture, GestureAction>> = _gestureMappings.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var lastTriggeredGesture = HandGesture.NONE
    private var lastTriggerTime = 0L

    fun updateGestureMapping(gesture: HandGesture, action: GestureAction) {
        val current = _gestureMappings.value.toMutableMap()
        current[gesture] = action
        _gestureMappings.value = current
    }

    fun onGestureDetected(gesture: HandGesture, confidence: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentGesture = gesture,
                confidence = confidence
            )

            val currentTime = System.currentTimeMillis()
            if (gesture == HandGesture.NONE) {
                lastTriggeredGesture = HandGesture.NONE
                return@launch
            }

            if (gesture == lastTriggeredGesture && currentTime - lastTriggerTime < 1500) {
                return@launch
            }

            executeGestureCommand(gesture)
            lastTriggeredGesture = gesture
            lastTriggerTime = currentTime
        }
    }

    private fun executeGestureCommand(gesture: HandGesture) {
        val action = _gestureMappings.value[gesture] ?: GestureAction.NONE
        if (action == GestureAction.NONE) return

        val timestamp = timeFormat.format(Date())
        var actionDescription = action.description

        when (action) {
            GestureAction.TURN_ON_ALL_LIGHTS -> {
                deviceRepository.devices.value
                    .filter { it.type == DeviceType.LIGHT }
                    .forEach { light ->
                        if (!light.isOn) {
                            deviceRepository.toggleDevice(light.id)
                        }
                    }
            }
            GestureAction.TURN_OFF_ALL_LIGHTS -> {
                deviceRepository.devices.value
                    .filter { it.type == DeviceType.LIGHT }
                    .forEach { light ->
                        if (light.isOn) {
                            deviceRepository.toggleDevice(light.id)
                        }
                    }
            }
            GestureAction.TURN_ON_LIVING_FAN -> {
                val fan = deviceRepository.devices.value.find { it.id == "fan_living" }
                if (fan != null && !fan.isOn) {
                    deviceRepository.toggleDevice("fan_living")
                }
            }
            GestureAction.TURN_OFF_LIVING_FAN -> {
                val fan = deviceRepository.devices.value.find { it.id == "fan_living" }
                if (fan != null && fan.isOn) {
                    deviceRepository.toggleDevice("fan_living")
                }
            }
            GestureAction.OPEN_MAIN_DOOR -> {
                val door = deviceRepository.devices.value.find { it.id == "door_main" }
                if (door != null && !door.isOpen) {
                    deviceRepository.toggleDoor("door_main")
                }
            }
            GestureAction.CLOSE_MAIN_DOOR -> {
                val door = deviceRepository.devices.value.find { it.id == "door_main" }
                if (door != null && door.isOpen) {
                    deviceRepository.toggleDoor("door_main")
                }
            }
            GestureAction.INCREASE_LIVING_FAN_SPEED -> {
                val fan = deviceRepository.devices.value.find { it.id == "fan_living" }
                if (fan != null) {
                    if (!fan.isOn) {
                        deviceRepository.toggleDevice("fan_living")
                    }
                    val nextSpeed = if (fan.speed >= 3) 1 else fan.speed + 1
                    deviceRepository.setFanSpeed("fan_living", nextSpeed)
                    actionDescription = "Tăng tốc độ quạt phòng khách lên cấp $nextSpeed"
                }
            }
            GestureAction.NONE -> {}
        }

        if (actionDescription.isNotEmpty()) {
            val logMessage = "[$timestamp] $actionDescription"
            val newHistory = listOf(logMessage) + _uiState.value.commandHistory
            _uiState.value = _uiState.value.copy(
                commandHistory = newHistory.take(10)
            )
        }
    }

    companion object {
        fun provideFactory(deviceRepository: DeviceRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GestureViewModel(deviceRepository) as T
                }
            }
    }
}
