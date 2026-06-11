package com.khangkietson.smarthome.ui.gesture.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

@Composable
fun HandOverlay(
    resultProvider: () -> GestureRecognizerResult?,
    modifier: Modifier = Modifier
) {
    val pointColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.fillMaxSize()) {
        val result = resultProvider()
        if (result == null) return@Canvas

        val landmarksList = result.landmarks()
        if (landmarksList.isEmpty()) return@Canvas

        for (handLandmarks in landmarksList) {
            val points = handLandmarks.map { landmark ->
                Offset(
                    x = landmark.x() * size.width,
                    y = landmark.y() * size.height
                )
            }

            drawHandSkeletalLines(points, lineColor)

            points.forEach { point ->
                drawCircle(
                    color = pointColor,
                    radius = 8f,
                    center = point
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandSkeletalLines(
    points: List<Offset>,
    color: Color
) {
    if (points.size < 21) return

    val strokeWidth = 5f

    val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        9 to 10, 10 to 11, 11 to 12,
        13 to 14, 14 to 15, 15 to 16,
        0 to 17, 17 to 18, 18 to 19, 19 to 20,
        5 to 9, 9 to 13, 13 to 17
    )

    connections.forEach { (start, end) ->
        if (start < points.size && end < points.size) {
            drawLine(
                color = color,
                start = points[start],
                end = points[end],
                strokeWidth = strokeWidth
            )
        }
    }
}
