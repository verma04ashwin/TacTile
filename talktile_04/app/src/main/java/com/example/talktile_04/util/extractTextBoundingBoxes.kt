package com.example.talktile_04.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

fun extractTextBoundingBoxes(pdfFile: File, pageNumber: Int = 1): List<TextElement> {
    val document = PDDocument.load(pdfFile)
    val stripper = BoundingBoxTextStripper()

    stripper.startPage = pageNumber
    stripper.endPage = pageNumber
    stripper.getText(document)

    document.close()
    return stripper.textElements
}
