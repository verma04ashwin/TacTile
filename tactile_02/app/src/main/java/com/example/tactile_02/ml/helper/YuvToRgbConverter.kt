package com.example.tactile_02.ml.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class YuvToRgbConverter {

    /**
     * Converts YUV_420_888 ImageProxy to ARGB Bitmap.
     */
    @OptIn(ExperimentalGetImage::class)
    fun yuvToRgb(imageProxy: ImageProxy): Bitmap {
        val image = imageProxy.image ?: throw IllegalArgumentException("ImageProxy has no image")

        val width = imageProxy.width
        val height = imageProxy.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val nv21 = yuv420ToNv21(image)
        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            width,
            height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val jpegBytes = out.toByteArray()
        val tempBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        // Copy into pre-allocated bitmap
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(tempBitmap, 0f, 0f, null)

        tempBitmap.recycle()
        return bitmap
    }

    /**
     * Converts YUV420_888 planes to NV21 format.
     */
    private fun yuv420ToNv21(image: Image): ByteArray {
        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}