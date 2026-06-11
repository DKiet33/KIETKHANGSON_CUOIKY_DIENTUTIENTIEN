package com.khangkietson.smarthome.data.model

data class Scene(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val actions: List<DeviceAction>,
    val isActive: Boolean = false,
    val trigger: SceneTrigger? = null  // AI trigger
)

data class DeviceAction(
    val deviceId: String,
    val turnOn: Boolean,
    val brightness: Int? = null,
    val speed: Int? = null,
    val open: Boolean? = null
)

enum class SceneTrigger {
    TIME_BASED,      // Hourly/time-based
    BEHAVIOR_BASED,  // User behavior-based (AI)
    MANUAL           // Manual trigger
}
