package com.example.talktile_05.services

import android.content.Context
import android.graphics.RectF
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripperByArea
import java.io.File
import java.io.FileOutputStream

class PdfTextExtractor(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractParagraphsFromAsset(assetPath: String, pageIndex: Int): List<String> {
        Log.d("PDF_EXTRACT", "Extracting page $pageIndex from: $assetPath")

        // Copy asset → cache (PDFBox cannot read assets directly)
        val cacheFile = File(context.cacheDir, assetPath.replace("/", "_"))
        if (!cacheFile.exists()) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { out -> input.copyTo(out) }
            }
        }

        val document = PDDocument.load(cacheFile)
        val page = document.getPage(pageIndex)
        val mediaBox = page.mediaBox

        val width = mediaBox.width
        val height = mediaBox.height

        // -----------------------------
        // FIXED TWO-COLUMN DETECTION
        // -----------------------------
        val leftCol = RectF(
            0f,
            0f,
            width * 0.48f,
            height
        )

        val rightCol = RectF(
            width * 0.48f,
            0f,
            width,
            height
        )

        val stripper = PDFTextStripperByArea()
        stripper.sortByPosition = true
        stripper.addRegion("LEFT", leftCol)
        stripper.addRegion("RIGHT", rightCol)
        stripper.extractRegions(page)

        val leftText = stripper.getTextForRegion("LEFT") ?: ""
        val rightText = stripper.getTextForRegion("RIGHT") ?: ""

        document.close()

        val fullText = (leftText + "\n" + rightText)

        return rebuildParas(cleanText(fullText))
    }

    // Remove garbage, fix hyphens, normalize spacing
    private fun cleanText(text: String): List<String> {
        return text
            .replace("\r", "")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map {
                if (it.endsWith("-")) {
                    it.dropLast(1)        // merge hyphen break
                } else it
            }
    }

    // Merge lines into paragraphs
    private fun rebuildParas(lines: List<String>): List<String> {
        val paras = mutableListOf<String>()
        val sb = StringBuilder()

        for (line in lines) {

            val l = line.trim()

            // If line ends with sentence end → commit paragraph
            if (l.endsWith(".") || l.endsWith("!") || l.endsWith("?")) {
                sb.append(l).append(" ")
                paras.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(l).append(" ")
            }
        }

        if (sb.isNotEmpty()) paras.add(sb.toString().trim())

        return paras
    }
}
