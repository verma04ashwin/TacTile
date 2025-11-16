package com.example.talktile_04.util

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.talktile_04.data.pdfExtractor.extractPdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PdfBoundingBoxViewer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var boundingBoxes by remember { mutableStateOf<List<TextElement>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pdfFile = extractPdfFile(context)
            pdfFile?.let {
                val boxes = extractTextBoundingBoxes(it, pageNumber = 1)
                boundingBoxes = boxes
                boxes.forEach { elem ->
                    Log.d(
                        "PDFBoxDemo",
                        "Text='${elem.text}' | Page=${elem.pageNumber} | x=${elem.x}, y=${elem.y}, w=${elem.width}, h=${elem.height}"
                    )
                }
            }
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        if (boundingBoxes.isEmpty()) {
            Text("Extracting PDF text...")
        } else {
            boundingBoxes.forEach { elem ->
                Text(
                    text = "Text='${elem.text}' | Page=${elem.pageNumber} | x=${elem.x}, y=${elem.y}, w=${elem.width}, h=${elem.height}",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}
