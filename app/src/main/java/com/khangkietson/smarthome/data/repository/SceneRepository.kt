package com.khangkietson.smarthome.data.repository

import com.khangkietson.smarthome.data.model.DeviceAction
import com.khangkietson.smarthome.data.model.DeviceType
import com.khangkietson.smarthome.data.model.Scene
import com.khangkietson.smarthome.data.model.SceneTrigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SceneRepository(private val deviceRepository: DeviceRepository) {
    private val _scenes = MutableStateFlow<List<Scene>>(emptyList())
    val scenes: StateFlow<List<Scene>> = _scenes.asStateFlow()

    init {
        val defaultScenes = listOf(
            Scene(
                id = "scene_sleep",
                name = "Đi ngủ",
                description = "Tắt tất cả đèn và quạt, khóa cửa chính.",
                icon = "sleep",
                actions = listOf(
                    DeviceAction("light_living", turnOn = false),
                    DeviceAction("light_bed", turnOn = false),
                    DeviceAction("light_kitchen", turnOn = false),
                    DeviceAction("fan_living", turnOn = false),
                    DeviceAction("fan_bed", turnOn = false),
                    DeviceAction("door_main", turnOn = false, open = false)
                ),
                trigger = SceneTrigger.TIME_BASED
            ),
            Scene(
                id = "scene_work",
                name = "Đi làm",
                description = "Tắt tất cả thiết bị điện, đóng cửa chính để đi làm.",
                icon = "work",
                actions = listOf(
                    DeviceAction("light_living", turnOn = false),
                    DeviceAction("light_bed", turnOn = false),
                    DeviceAction("light_kitchen", turnOn = false),
                    DeviceAction("fan_living", turnOn = false),
                    DeviceAction("fan_bed", turnOn = false),
                    DeviceAction("door_main", turnOn = false, open = false)
                ),
                trigger = SceneTrigger.BEHAVIOR_BASED
            ),
            Scene(
                id = "scene_home",
                name = "Về nhà",
                description = "Mở cửa chính, bật đèn và quạt phòng khách.",
                icon = "home",
                actions = listOf(
                    DeviceAction("light_living", turnOn = true, brightness = 80),
                    DeviceAction("fan_living", turnOn = true, speed = 2),
                    DeviceAction("door_main", turnOn = true, open = true)
                ),
                trigger = SceneTrigger.MANUAL
            )
        )
        _scenes.value = defaultScenes
    }

    fun activateScene(sceneId: String) {
        val scene = _scenes.value.find { it.id == sceneId } ?: return
        
        scene.actions.forEach { action ->
            val currentDevice = deviceRepository.devices.value.find { it.id == action.deviceId }
            if (currentDevice != null) {
                if (action.turnOn) {
                    when (currentDevice.type) {
                        DeviceType.LIGHT -> {
                            val targetBrightness = action.brightness ?: currentDevice.brightness
                            deviceRepository.setBrightness(action.deviceId, targetBrightness)
                        }
                        DeviceType.FAN -> {
                            val targetSpeed = action.speed ?: currentDevice.speed
                            deviceRepository.setFanSpeed(action.deviceId, targetSpeed)
                        }
                        DeviceType.DOOR -> {
                            if (!currentDevice.isOpen) {
                                deviceRepository.toggleDoor(action.deviceId)
                            }
                        }
                    }
                } else {
                    when (currentDevice.type) {
                        DeviceType.LIGHT, DeviceType.FAN -> {
                            if (currentDevice.isOn) {
                                deviceRepository.toggleDevice(action.deviceId)
                            }
                        }
                        DeviceType.DOOR -> {
                            if (currentDevice.isOpen) {
                                deviceRepository.toggleDoor(action.deviceId)
                            }
                        }
                    }
                }
            }
        }

        _scenes.value = _scenes.value.map { s ->
            s.copy(isActive = s.id == sceneId)
        }
    }
}
