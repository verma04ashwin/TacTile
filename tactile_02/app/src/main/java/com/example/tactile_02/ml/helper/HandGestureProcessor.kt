package com.example.tactile_02.ml.gesture

import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

class HandGestureProcessor {

    /**
     * Detects "closed_fist" when:
     * 1. Fingers are folded (avg distance from wrist < threshold)
     * 2. Back of hand faces camera (palm down).
     */
    fun classify(landmarks: List<NormalizedLandmark>): String? {
        // --- 1. Compute average fingertip distance from wrist ---
        val wrist = landmarks[0]
        val fingertips = listOf(landmarks[8], landmarks[12], landmarks[16], landmarks[20])
        val avgDistance = fingertips.map { distance(wrist, it) }.average().toFloat()

        // --- 2. Compute palm orientation ---
        val indexBase = landmarks[5]
        val pinkyBase = landmarks[17]
        val palmVector = crossProduct(
            vector(wrist, indexBase),
            vector(wrist, pinkyBase)
        )

        // For this camera orientation: palmVector.z > 0 → back of hand facing camera (palm down)
        val palmFacingDown = palmVector.z > 0

        Log.d("GestureDebug", "avgDist=$avgDistance palmZ=${palmVector.z} down=$palmFacingDown")

        // --- 3. Classification ---
        return if (avgDistance < 0.22f && palmFacingDown) {
            "closed_fist"
        } else {
            null
        }
    }

    private fun distance(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        return sqrt(
            (a.x() - b.x()) * (a.x() - b.x()) +
                    (a.y() - b.y()) * (a.y() - b.y())
        )
    }

    private data class Vec3(val x: Float, val y: Float, val z: Float)

    private fun vector(a: NormalizedLandmark, b: NormalizedLandmark): Vec3 {
        return Vec3(b.x() - a.x(), b.y() - a.y(), b.z() - a.z())
    }

    private fun crossProduct(v1: Vec3, v2: Vec3): Vec3 {
        return Vec3(
            v1.y * v2.z - v1.z * v2.y,
            v1.z * v2.x - v1.x * v2.z,
            v1.x * v2.y - v1.y * v2.x
        )
    }
}
