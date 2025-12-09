package com.example.talktile_05.utils

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log

/**
 * BoundingBoxProcessor
 *
 * Takes:
 *  - The original camera bitmap
 *  - YOLO's map bounding box
 *  - YOLO's finger bounding box
 *
 * Produces:
 *  - Cropped & scaled 640×640 map image
 *  - Finger coordinates mapped into the canonical 640×640 map space
 */
object BoundingBoxProcessor {

    private const val TARGET_SIZE = 640

    /**
     * Adjust the finger center relative to the map crop.
     *
     * Returns:
     *   Pair( resizedMapBitmap, Pair(centerX, centerY) )
     */
    fun processAndAdjustFingerCenter(
        originalBitmap: Bitmap,
        mapBox: RectF,
        fingerBox: RectF
    ): Pair<Bitmap, Pair<Float, Float>> {

        // 1. Crop the map region from camera image
        val cropLeft = mapBox.left.toInt().coerceAtLeast(0)
        val cropTop = mapBox.top.toInt().coerceAtLeast(0)
        val cropWidth = mapBox.width().toInt().coerceAtMost(originalBitmap.width - cropLeft)
        val cropHeight = mapBox.height().toInt().coerceAtMost(originalBitmap.height - cropTop)

        if (cropWidth <= 0 || cropHeight <= 0) {
            Log.e("BoundingBoxProcessor", "Invalid crop dimensions.")
            return Pair(originalBitmap, Pair(-1f, -1f))
        }

        val cropped = Bitmap.createBitmap(
            originalBitmap,
            cropLeft,
            cropTop,
            cropWidth,
            cropHeight
        )

        // 2. Finger center relative to *original* camera frame
        val rawCenterX = fingerBox.centerX()
        val rawCenterY = fingerBox.centerY()

        // 3. Convert to coordinates inside the cropped map
        val relativeX = rawCenterX - mapBox.left
        val relativeY = rawCenterY - mapBox.top

        // 4. Resize map to 640×640 (canonical)
        val resizedMap = Bitmap.createScaledBitmap(cropped, TARGET_SIZE, TARGET_SIZE, true)

        // 5. Scale finger center accordingly
        val scaleX = TARGET_SIZE.toFloat() / cropWidth
        val scaleY = TARGET_SIZE.toFloat() / cropHeight

        val newCenterX = relativeX * scaleX
        val newCenterY = relativeY * scaleY

        Log.d("BoundingBoxProcessor", "Finger → Canonical ($newCenterX, $newCenterY)")

        return Pair(resizedMap, Pair(newCenterX, newCenterY))
    }
}
