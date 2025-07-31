package com.example.tactile_02.utils

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log

object BoundingBoxProcessor {

    private const val TARGET_SIZE = 640

    /**
     * Adjust finger bounding box center relative to map crop and scale.
     * Returns:
     *   - Cropped/Resized Map Bitmap
     *   - Adjusted Finger Center (x, y)
     */
    fun processAndAdjustFingerCenter(
        originalBitmap: Bitmap,
        mapBox: RectF,
        fingerBox: RectF
    ): Pair<Bitmap, Pair<Float, Float>> {

        // Crop mapRegion
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            mapBox.left.toInt().coerceAtLeast(0),
            mapBox.top.toInt().coerceAtLeast(0),
            mapBox.width().toInt().coerceAtMost(originalBitmap.width - mapBox.left.toInt()),
            mapBox.height().toInt().coerceAtMost(originalBitmap.height - mapBox.top.toInt())
        )

        // Finger center relative to crop
        val centerX = fingerBox.centerX() - mapBox.left
        val centerY = fingerBox.centerY() - mapBox.top

        // Resize map
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, TARGET_SIZE, TARGET_SIZE, true)

        // Scale finger center to resized image
        val scaleX = TARGET_SIZE.toFloat() / croppedBitmap.width
        val scaleY = TARGET_SIZE.toFloat() / croppedBitmap.height
        val newCenterX = centerX * scaleX
        val newCenterY = centerY * scaleY

        Log.d("FingerCenter", "Adjusted center: ($newCenterX, $newCenterY)")

        return Pair(resizedBitmap, Pair(newCenterX, newCenterY))
    }
}
