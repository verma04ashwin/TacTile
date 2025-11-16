package com.example.talktile_04.data.pdfExtractor

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun extractPdfFile(): File? {
    val context = LocalContext.current
    val assetManager = context.assets
    val fileName = "fees101.pdf"

    return try {
        // 1️⃣ Create a destination file inside app's cacheDir
        val file = File(context.cacheDir, fileName)

        // 2️⃣ Copy the PDF from assets into cacheDir
        assetManager.open(fileName).use { input: InputStream ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        // 3️⃣ Return the file (PDFBox can read this)
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
