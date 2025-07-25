package com.example.tactile_main.handlandmarkerhelping

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FrameSaver {

    fun saveFrame(context: Context, bitmap: Bitmap) {
        try {
            val dir = File(context.getExternalFilesDir(null), "frames")
            if (!dir.exists()) dir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val file = File(dir, "cropped_$timeStamp.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Log.d("FrameSaver", "✅ Cropped frame saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FrameSaver", "❌ Failed to save cropped frame", e)
        }
    }
}