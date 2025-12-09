package com.example.talktile_05.ml.helper

import android.graphics.*
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Converts ImageProxy (YUV_420_888) → ARGB Bitmap
 * Used by TFLiteHelper to feed YOLO.
 */
class YuvToRgbConverter {

    @OptIn(ExperimentalGetImage::class)
    fun yuvToRgb(imageProxy: ImageProxy): Bitmap {
        val image = imageProxy.image ?: throw IllegalArgumentException("ImageProxy has no image")

        val width = imageProxy.width
        val height = imageProxy.height
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val nv21 = yuv420ToNv21(image)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)

        val jpegBytes = out.toByteArray()
        val temp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        val canvas = Canvas(outBitmap)
        canvas.drawBitmap(temp, 0f, 0f, null)
        temp.recycle()

        return outBitmap
    }

    /**
     * Convert YUV420_888 → NV21 byte array
     */
    private fun yuv420ToNv21(image: android.media.Image): ByteArray {
        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U/V swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}
