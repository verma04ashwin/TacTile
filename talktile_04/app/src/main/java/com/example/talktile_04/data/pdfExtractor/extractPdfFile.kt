package com.example.talktile_04.data.pdfExtractor

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun extractPdfFile(context: Context): File? {
    val assetManager = context.assets
    val fileName = "fees101.pdf"

    return try {
        val file = File(context.cacheDir, fileName)
        assetManager.open(fileName).use { input: InputStream ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
