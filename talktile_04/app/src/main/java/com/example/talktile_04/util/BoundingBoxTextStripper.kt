package com.example.talktile_04.util

import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs

class BoundingBoxTextStripper : PDFTextStripper() {

    val textElements = mutableListOf<TextElement>()
    private var lastY = -1f
    private val currentLine = mutableListOf<TextPosition>()

    override fun processTextPosition(text: TextPosition?) {
        if (text == null) return

        val y = text.yDirAdj

        // Detect new line
        if (lastY < 0) {
            lastY = y
        }

        // If vertical distance > threshold → new line
        if (abs(lastY - y) > 2.0f) {
            flushCurrentLine()
            currentLine.clear()
            lastY = y
        }

        currentLine.add(text)
    }

    // ✅ Correct method signature for Tom Roush PDFBox
    override fun endPage(page: PDPage?) {
        flushCurrentLine()
        currentLine.clear()
        lastY = -1f
        super.endPage(page)
    }

    private fun flushCurrentLine() {
        if (currentLine.isEmpty()) return

        // Sort left → right
        currentLine.sortBy { it.xDirAdj }

        val words = mutableListOf<TextElement>()
        var currentWord = StringBuilder()
        var startX = currentLine.first().xDirAdj
        var lastXEnd = startX
        var maxHeight = 0f

        for (tp in currentLine) {
            val spacing = tp.xDirAdj - lastXEnd
            val avgCharWidth = tp.widthDirAdj / (tp.unicode.length.coerceAtLeast(1))

            // Adjust this multiplier (increase → fewer splits)
            val threshold = avgCharWidth * 5f

            if (spacing > threshold) {
                if (currentWord.isNotEmpty()) {
                    words.add(
                        TextElement(
                            text = currentWord.toString(),
                            x = startX,
                            y = lastY,
                            width = lastXEnd - startX,
                            height = maxHeight,
                            pageNumber = currentPageNo
                        )
                    )
                }
                currentWord = StringBuilder()
                startX = tp.xDirAdj
                maxHeight = 0f
            }

            currentWord.append(tp.unicode)
            lastXEnd = tp.xDirAdj + tp.widthDirAdj
            maxHeight = maxOf(maxHeight, tp.heightDir)
        }

        if (currentWord.isNotEmpty()) {
            words.add(
                TextElement(
                    text = currentWord.toString(),
                    x = startX,
                    y = lastY,
                    width = lastXEnd - startX,
                    height = maxHeight,
                    pageNumber = currentPageNo
                )
            )
        }

        textElements.addAll(words)
    }
}
