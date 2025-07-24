package com.example.tactile_main

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PreprocessedData(
    val buffer: ByteBuffer,
    val resizedBitmap: Bitmap
)

fun preprocess(bitmap: Bitmap, inputSize: Int = 640): PreprocessedData {
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
    val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
    byteBuffer.order(ByteOrder.nativeOrder())

    for (y in 0 until inputSize) {
        for (x in 0 until inputSize) {
            val pixel = resizedBitmap.getPixel(x, y)
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
    }

    return PreprocessedData(byteBuffer, resizedBitmap)
}
