package com.example.testingtflitemodels

import android.content.Context
import android.graphics.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteHelper(context: Context, modelName: String) {

    private val inputImageSize = 640
    private val interpreter: Interpreter

    init {
        val modelBuffer = loadModelFile(context, modelName)
        val options = Interpreter.Options()
        interpreter = Interpreter(modelBuffer, options)
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageSize, inputImageSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputImageSize * inputImageSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until inputImageSize) {
            for (x in 0 until inputImageSize) {
                val pixel = resizedBitmap.getPixel(x, y)
                val r = (pixel shr 16 and 0xFF) / 255.0f
                val g = (pixel shr 8 and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }

        return byteBuffer
    }

    // Returns: (bitmapWithBoxDrawn, croppedDetectedRegionOrNull)
    fun runInference(bitmap: Bitmap): Pair<Bitmap, Bitmap?> {
        val inputBuffer = preprocessImage(bitmap)

        val rawOutput = Array(1) { Array(5) { FloatArray(8400) } }
        interpreter.run(inputBuffer, rawOutput)

        val outputShape = Array(8400) { FloatArray(6) }
        for (i in 0 until 8400) {
            for (j in 0 until 5) {
                outputShape[i][j] = rawOutput[0][j][i]
            }
            outputShape[i][5] = 0f
        }

        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()

        val topDetection = outputShape.maxByOrNull { it[4] }

        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.RED
            strokeWidth = 4f
        }

        val confThreshold = 0.5f
        var croppedBitmap: Bitmap? = null

        if (topDetection != null && topDetection[4] > confThreshold) {
            val x = topDetection[0] * originalWidth
            val y = topDetection[1] * originalHeight
            val w = topDetection[2] * originalWidth
            val h = topDetection[3] * originalHeight

            val left = (x - w / 2f).coerceIn(0f, originalWidth - 1)
            val top = (y - h / 2f).coerceIn(0f, originalHeight - 1)
            val right = (x + w / 2f).coerceIn(1f, originalWidth)
            val bottom = (y + h / 2f).coerceIn(1f, originalHeight)

            canvas.drawRect(left, top, right, bottom, paint)

            croppedBitmap = Bitmap.createBitmap(
                bitmap,
                left.toInt(),
                top.toInt(),
                (right - left).toInt(),
                (bottom - top).toInt()
            )
        }

        return Pair(resultBitmap, croppedBitmap)
    }

    fun close() {
        interpreter.close()
    }
}
