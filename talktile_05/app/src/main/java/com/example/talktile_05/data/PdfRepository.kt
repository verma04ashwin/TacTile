package com.example.talktile_05.data

import android.content.res.AssetManager
import com.example.talktile_05.App
import android.util.Log

class PdfRepository(
    private val assets: AssetManager = App.instance.assets
) {

    /**
     * List top-level folders (books).
     */
    fun listBooks(): List<String> {
        return assets.list("")?.filter { folder ->
            // consider only directories that contain something
            assets.list(folder)?.isNotEmpty() == true
        } ?: emptyList()
    }

    /**
     * List chapters as folder names that contain a pdf file.
     * Works for folder-per-chapter pattern:
     * assets/<book>/<chapter>/...pdf
     */
    fun listChapters(book: String): List<String> {
        return try {
            assets.list(book)?.filter { chapterName ->
                val files = assets.list("$book/$chapterName") ?: arrayOf()
                files.any { it.endsWith(".pdf") }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Resolve exact asset path to PDF inside folder.
     * Example return:
     * "contemporary india/Resources and Development/chapter.pdf"
     */
    fun pdfPathFor(book: String, chapter: String): String {
        val dir = "$book/$chapter"
        val files = assets.list(dir) ?: arrayOf()

        val pdf = files.firstOrNull { it.endsWith(".pdf") }
            ?: throw IllegalStateException("No PDF found in folder: $dir")

        val full = "$dir/$pdf"
        Log.d("PDF_REPO", "Resolved PDF = $full")
        return full
    }
}
