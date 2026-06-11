package com.khangkietson.smarthome.gesture

import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.khangkietson.smarthome.data.model.HandGesture
import com.khangkietson.smarthome.data.model.GestureCommand

class GestureClassifier {
    private var lastRawGesture = HandGesture.NONE
    private var lastGestureTime = 0L
    private var confirmedGesture = HandGesture.NONE

    fun classify(result: GestureRecognizerResult): GestureCommand {
        val gestures = result.gestures()
        val landmarksList = result.landmarks()

        // 1. Kiểm tra hình học của bàn tay trước (Cử chỉ giơ 2 ngón khép)
        val isTwoFingers = if (!landmarksList.isNullOrEmpty() && landmarksList[0].size >= 21) {
            val hand = landmarksList[0]
            val indexTip = hand[8]
            val indexPip = hand[6]
            val indexMcp = hand[5]
            
            val middleTip = hand[12]
            val middlePip = hand[10]
            val middleMcp = hand[9]
            
            val ringTip = hand[16]
            val ringPip = hand[14]
            val ringMcp = hand[13]
            
            val pinkyTip = hand[20]
            val pinkyPip = hand[18]
            val pinkyMcp = hand[17]

            // Ngón trỏ và ngón giữa duỗi (đầu ngón cao hơn khớp MCP và PIP)
            val indexExtended = indexTip.y() < indexMcp.y() && indexTip.y() < indexPip.y()
            val middleExtended = middleTip.y() < middleMcp.y() && middleTip.y() < middlePip.y()
            
            // Ngón áp út và ngón út gập (đầu ngón thấp hơn khớp)
            val ringFolded = ringTip.y() > ringMcp.y() || ringTip.y() > ringPip.y()
            val pinkyFolded = pinkyTip.y() > pinkyMcp.y() || pinkyTip.y() > pinkyPip.y()
            
            // Khoảng cách giữa 2 đầu ngón trỏ và giữa khép sát nhau (< 0.06)
            val dx = indexTip.x() - middleTip.x()
            val dy = indexTip.y() - middleTip.y()
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
            
            indexExtended && middleExtended && ringFolded && pinkyFolded && (distance < 0.06)
        } else {
            false
        }

        if (isTwoFingers) {
            val currentTime = System.currentTimeMillis()
            val rawGesture = HandGesture.TWO_FINGERS_CLOSED
            val score = 0.9f
            
            if (rawGesture != lastRawGesture) {
                lastRawGesture = rawGesture
                lastGestureTime = currentTime
            } else {
                if (currentTime - lastGestureTime >= 500 && rawGesture != confirmedGesture) {
                    confirmedGesture = rawGesture
                }
            }
            return GestureCommand(rawGesture, score, currentTime)
        }

        // 2. Chuyển tiếp sang bộ nhận diện mặc định của MediaPipe nếu không khớp cử chỉ tự định nghĩa
        if (gestures.isNullOrEmpty() || gestures[0].isNullOrEmpty()) {
            val currentTime = System.currentTimeMillis()
            if (HandGesture.NONE != lastRawGesture) {
                lastRawGesture = HandGesture.NONE
                lastGestureTime = currentTime
            } else {
                if (currentTime - lastGestureTime >= 500 && HandGesture.NONE != confirmedGesture) {
                    confirmedGesture = HandGesture.NONE
                }
            }
            return GestureCommand(HandGesture.NONE, 1.0f, currentTime)
        }

        val category = gestures[0][0]
        val categoryName = category.categoryName()
        val score = category.score()

        val rawGesture = when (categoryName) {
            "Open_Palm" -> HandGesture.OPEN_PALM
            "Closed_Fist" -> HandGesture.CLOSED_FIST
            "Thumbs_Up", "Thumb_Up" -> HandGesture.THUMB_UP
            "Thumbs_Down", "Thumb_Down" -> HandGesture.THUMB_DOWN
            "Victory" -> HandGesture.VICTORY
            "Pointing_Up" -> HandGesture.POINTING_UP
            else -> HandGesture.NONE
        }

        val currentTime = System.currentTimeMillis()
        if (rawGesture != lastRawGesture) {
            lastRawGesture = rawGesture
            lastGestureTime = currentTime
        } else {
            if (currentTime - lastGestureTime >= 500 && rawGesture != confirmedGesture) {
                confirmedGesture = rawGesture
            }
        }

        return GestureCommand(rawGesture, score, currentTime)
    }
    
    fun getConfirmedGesture(): HandGesture {
        return confirmedGesture
    }
    
    fun reset() {
        lastRawGesture = HandGesture.NONE
        lastGestureTime = 0L
        confirmedGesture = HandGesture.NONE
    }
}
