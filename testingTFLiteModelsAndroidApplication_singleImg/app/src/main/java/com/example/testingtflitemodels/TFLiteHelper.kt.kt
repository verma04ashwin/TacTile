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

        val byteBuffer = ByteBuffer.allocateDirect(1 * inputImageSize * inputImageSize * 3 * 4) // RGB
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

    fun runInference(bitmap: Bitmap): Bitmap {
        val inputBuffer = preprocessImage(bitmap)

        // Model output is [1, 5, 8400]
        val rawOutput = Array(1) { Array(5) { FloatArray(8400) } }
        interpreter.run(inputBuffer, rawOutput)

        // Transpose to [8400][6] (add dummy class index if needed)
        val outputShape = Array(8400) { FloatArray(6) }
        for (i in 0 until 8400) {
            for (j in 0 until 5) {
                outputShape[i][j] = rawOutput[0][j][i]
            }
            outputShape[i][5] = 0f // Dummy class index
        }

        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()

        val topDetection = outputShape.maxByOrNull { it[4] } // Highest confidence

        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.RED
            strokeWidth = 4f
        }

        val confThreshold = 0.5f

        if (topDetection != null && topDetection[4] > confThreshold) {
            val x = topDetection[0] * originalWidth
            val y = topDetection[1] * originalHeight
            val w = topDetection[2] * originalWidth
            val h = topDetection[3] * originalHeight

            val left = x - w / 2f
            val top = y - h / 2f
            val right = x + w / 2f
            val bottom = y + h / 2f

            canvas.drawRect(left, top, right, bottom, paint)
        }

        return resultBitmap
    }

    fun close() {
        interpreter.close()
    }
}
