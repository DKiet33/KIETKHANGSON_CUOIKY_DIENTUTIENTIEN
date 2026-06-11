package com.khangkietson.smarthome.data.model

enum class DeviceType { LIGHT, FAN, DOOR }

data class Device(
    val id: String,
    val name: String,
    val type: DeviceType,
    val room: String,
    val isOn: Boolean = false,
    val brightness: Int = 100,   // Only used for LIGHT (0-100)
    val speed: Int = 1,          // Only used for FAN (1-3)
    val isOpen: Boolean = false  // Only used for DOOR
)
