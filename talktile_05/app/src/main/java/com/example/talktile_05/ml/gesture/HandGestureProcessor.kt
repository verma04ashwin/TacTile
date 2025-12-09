package com.example.talktile_05.ml.gesture

import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class HandGestureProcessor {

    fun classify(landmarks: List<NormalizedLandmark>): String? {
        if (landmarks.size < 21) return null

        // Landmarks
        val wrist = landmarks[0]
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val ringTip = landmarks[16]
        val pinkyTip = landmarks[20]

        fun dist(a: Float, b: Float) = kotlin.math.abs(a - b)

        // Use .y() instead of .y
        val indexOpen = dist(indexTip.y(), wrist.y()) > 0.15f
        val middleOpen = dist(middleTip.y(), wrist.y()) > 0.15f
        val ringOpen = dist(ringTip.y(), wrist.y()) > 0.15f
        val pinkyOpen = dist(pinkyTip.y(), wrist.y()) > 0.15f

        // Thumb comparison uses x()
        val thumbOpen = dist(thumbTip.x(), wrist.x()) > 0.1f

        Log.d(
            "GestureProcessor",
            "IndexOpen=$indexOpen MiddleOpen=$middleOpen RingOpen=$ringOpen " +
                    "PinkyOpen=$pinkyOpen ThumbOpen=$thumbOpen"
        )

        // --------------------------
        // Simple Gesture Rules
        // --------------------------

        // Open Palm
        if (indexOpen && middleOpen && ringOpen && pinkyOpen) {
            return "Open Palm"
        }

        // Fist
        if (!indexOpen && !middleOpen && !ringOpen && !pinkyOpen) {
            return "Fist"
        }

        // Pointing
        if (indexOpen && !middleOpen && !ringOpen && !pinkyOpen) {
            return "Pointing Gesture"
        }

        // Thumb Up (optional)
        if (thumbOpen && !indexOpen && !middleOpen) {
            return "Thumb Up"
        }

        return null
    }
}
