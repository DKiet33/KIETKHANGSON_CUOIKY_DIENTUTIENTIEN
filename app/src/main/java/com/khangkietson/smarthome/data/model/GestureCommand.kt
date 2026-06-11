package com.khangkietson.smarthome.data.model

enum class HandGesture {
    OPEN_PALM,      // Open palm -> Turn on all lights
    CLOSED_FIST,    // Fist -> Turn off all lights
    THUMB_UP,       // Thumb up -> Turn on fan
    THUMB_DOWN,     // Thumb down -> Turn off fan
    VICTORY,        // V sign -> Open door
    POINTING_UP,    // Pointing up -> Increase fan speed
    TWO_FINGERS_CLOSED, // Two fingers closed -> Close door
    NONE            // Gesture not recognized
}

data class GestureCommand(
    val gesture: HandGesture,
    val confidence: Float,     // Confidence level 0.0 to 1.0
    val timestamp: Long
)
