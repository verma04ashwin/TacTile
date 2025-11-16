package com.example.talktile_04

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.talktile_04.ui.theme.Talktile_04Theme
import com.example.talktile_04.util.PdfBoundingBoxViewer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()

        setContent {
            Talktile_04Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PdfBoundingBoxViewer(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
