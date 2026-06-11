package com.khangkietson.smarthome.data.repository

import android.content.Context
import com.khangkietson.smarthome.data.model.Device
import com.khangkietson.smarthome.data.model.DeviceType
import com.khangkietson.smarthome.data.mqtt.MqttService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class DeviceRepository(private val context: Context, private val mqttService: MqttService) {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val lastLocalChangeTime = ConcurrentHashMap<String, Long>()
    private val sharedPreferences = context.getSharedPreferences("smarthome_device_states", Context.MODE_PRIVATE)

    init {
        val initialList = listOf(
            Device(
                "light_living", "Đèn phòng khách", DeviceType.LIGHT, "Phòng khách",
                isOn = sharedPreferences.getBoolean("light_living_on", false),
                brightness = sharedPreferences.getInt("light_living_brightness", 80)
            ),
            Device(
                "light_bed", "Đèn phòng ngủ", DeviceType.LIGHT, "Phòng ngủ",
                isOn = sharedPreferences.getBoolean("light_bed_on", false),
                brightness = sharedPreferences.getInt("light_bed_brightness", 60)
            ),
            Device(
                "light_kitchen", "Đèn nhà bếp", DeviceType.LIGHT, "Nhà bếp",
                isOn = sharedPreferences.getBoolean("light_kitchen_on", false),
                brightness = sharedPreferences.getInt("light_kitchen_brightness", 90)
            ),
            Device(
                "fan_living", "Quạt phòng khách", DeviceType.FAN, "Phòng khách",
                isOn = sharedPreferences.getBoolean("fan_living_on", false),
                speed = sharedPreferences.getInt("fan_living_speed", 2)
            ),
            Device(
                "fan_bed", "Quạt phòng ngủ", DeviceType.FAN, "Phòng ngủ",
                isOn = sharedPreferences.getBoolean("fan_bed_on", false),
                speed = sharedPreferences.getInt("fan_bed_speed", 1)
            ),
            Device(
                "door_main", "Cửa chính", DeviceType.DOOR, "Lối vào",
                isOpen = sharedPreferences.getBoolean("door_main_open", false),
                isOn = sharedPreferences.getBoolean("door_main_open", false)
            )
        )
        _devices.value = initialList

        initialList.forEach { device ->
            // Tất cả các thiết bị đều subscribe topic phản hồi trạng thái 'state'
            mqttService.subscribe("smarthome/device/${device.id}/state") { payload ->
                updateDeviceStateFromPayload(device.id, payload)
            }
        }
    }

    private fun isHardwareDevice(id: String): Boolean {
        return id == "light_living" || id == "light_bed" || id == "light_kitchen" ||
                id == "fan_living" || id == "fan_bed" || id == "door_main"
    }

    private fun getPublishTopic(id: String): String {
        return if (isHardwareDevice(id)) {
            "smarthome/device/$id/set"
        } else {
            "smarthome/device/$id/state"
        }
    }

    private fun saveDeviceState(device: Device) {
        sharedPreferences.edit().apply {
            putBoolean("${device.id}_on", device.isOn)
            if (device.type == DeviceType.LIGHT) {
                putInt("${device.id}_brightness", device.brightness)
            } else if (device.type == DeviceType.FAN) {
                putInt("${device.id}_speed", device.speed)
            } else if (device.type == DeviceType.DOOR) {
                putBoolean("${device.id}_open", device.isOpen)
            }
            apply()
        }
    }

    private fun updateDeviceInList(updatedDevice: Device) {
        _devices.value = _devices.value.map {
            if (it.id == updatedDevice.id) updatedDevice else it
        }
        saveDeviceState(updatedDevice)
    }

    fun toggleDevice(id: String) {
        val device = _devices.value.find { it.id == id } ?: return
        val nextOnState = !device.isOn
        
        val updatedDevice = if (device.type == DeviceType.DOOR) {
            device.copy(isOpen = nextOnState, isOn = nextOnState)
        } else {
            device.copy(isOn = nextOnState)
        }
        lastLocalChangeTime[id] = System.currentTimeMillis()
        updateDeviceInList(updatedDevice)

        val topic = getPublishTopic(id)
        if (device.type == DeviceType.DOOR) {
            val payload = if (nextOnState) "OPEN" else "CLOSE"
            mqttService.publish(topic, payload)
        } else {
            val payload = if (nextOnState) "ON" else "OFF"
            mqttService.publish(topic, payload)
        }
    }

    fun setBrightness(id: String, brightness: Int) {
        val clampedBrightness = brightness.coerceIn(0, 100)
        val device = _devices.value.find { it.id == id }
        if (device != null) {
            val updatedDevice = device.copy(isOn = true, brightness = clampedBrightness)
            lastLocalChangeTime[id] = System.currentTimeMillis()
            updateDeviceInList(updatedDevice)
        }
        val topic = getPublishTopic(id)
        val payload = "ON,BRIGHTNESS=$clampedBrightness"
        mqttService.publish(topic, payload)
    }

    fun setFanSpeed(id: String, speed: Int) {
        val clampedSpeed = speed.coerceIn(1, 3)
        val device = _devices.value.find { it.id == id }
        if (device != null) {
            val updatedDevice = device.copy(isOn = true, speed = clampedSpeed)
            lastLocalChangeTime[id] = System.currentTimeMillis()
            updateDeviceInList(updatedDevice)
        }
        val topic = getPublishTopic(id)
        val payload = "ON,SPEED=$clampedSpeed"
        mqttService.publish(topic, payload)
    }

    fun toggleDoor(id: String) {
        val device = _devices.value.find { it.id == id && it.type == DeviceType.DOOR } ?: return
        val nextOpenState = !device.isOpen
        val updatedDevice = device.copy(isOpen = nextOpenState, isOn = nextOpenState)
        lastLocalChangeTime[id] = System.currentTimeMillis()
        updateDeviceInList(updatedDevice)

        val topic = getPublishTopic(id)
        val payload = if (nextOpenState) "OPEN" else "CLOSE"
        mqttService.publish(topic, payload)
    }

    private fun updateDeviceStateFromPayload(id: String, payload: String) {
        val changeTime = lastLocalChangeTime[id] ?: 0L
        if (System.currentTimeMillis() - changeTime < 1000) {
            return
        }

        _devices.value = _devices.value.map { device ->
            if (device.id == id) {
                val updated = when {
                    payload == "ON" -> device.copy(isOn = true)
                    payload == "OFF" -> device.copy(isOn = false)
                    payload == "OPEN" -> device.copy(isOpen = true, isOn = true)
                    payload == "CLOSE" -> device.copy(isOpen = false, isOn = false)
                    payload.startsWith("ON,BRIGHTNESS=") -> {
                        val brightness = payload.substringAfter("ON,BRIGHTNESS=").toIntOrNull() ?: device.brightness
                        device.copy(isOn = true, brightness = brightness)
                    }
                    payload.startsWith("ON,SPEED=") -> {
                        val speed = payload.substringAfter("ON,SPEED=").toIntOrNull() ?: device.speed
                        device.copy(isOn = true, speed = speed)
                    }
                    else -> device
                }
                if (updated != device) {
                    saveDeviceState(updated)
                }
                updated
            } else {
                device
            }
        }
    }
}
