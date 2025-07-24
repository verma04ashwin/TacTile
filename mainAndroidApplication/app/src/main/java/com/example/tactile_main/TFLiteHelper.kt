package com.example.tactile_main

import android.content.Context
import android.graphics.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.apply
import kotlin.collections.maxByOrNull
import kotlin.ranges.coerceIn
import kotlin.ranges.until

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

    // Only runs inference now. Image must already be preprocessed.
    fun runInference(preprocessedInput: ByteBuffer): FloatArray {
        val rawOutput = Array(1) { Array(5) { FloatArray(8400) } }
        interpreter.run(preprocessedInput, rawOutput)

        // Flatten and return highest scoring box
        val output = FloatArray(6)
        for (i in 0 until 8400) {
            val score = rawOutput[0][4][i]
            if (score > output[4]) {
                for (j in 0 until 5) output[j] = rawOutput[0][j][i]
                output[5] = i.toFloat() // Optional: store index
            }
        }
        return output
    }

    fun close() {
        interpreter.close()
    }
}
