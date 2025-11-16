package com.example.tactile_03

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

// Main entry point of the app
class MainActivity : ComponentActivity() {
    // Text-to-Speech (TTS) engine variable
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize PDFBox (needed for PDF parsing on Android)
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Text-to-Speech engine
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set language to US English once TTS is ready
                tts.language = Locale.US
            }
        }

        // Set the app's UI content using Jetpack Compose
        setContent {
            MaterialTheme { // Apply Material Design theme
                Surface(modifier = Modifier.fillMaxSize()) { // A full-screen container
                    PdfReaderScreen(context = this, tts = tts) // Show our custom PDF reader UI
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop and release TTS when activity is destroyed
        tts.stop()
        tts.shutdown()
    }
}

// Custom PDF stripper that extracts BOTH text and its position (bounding box info)
class PositionTextStripper : PDFTextStripper() {
    // List of pairs: (character, position info)
    val textWithPositions = mutableListOf<Pair<String, TextPosition>>()

    // This method is called for every character found in the PDF
    override fun processTextPosition(text: TextPosition?) {
        if (text != null) {
            // Save the unicode character + its position
            textWithPositions.add(Pair(text.unicode, text))
        }
        // Call parent method (keeps normal text processing)
        super.processTextPosition(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(context: Context, tts: TextToSpeech) {
    // List of available PDF files in assets
    val pdfFiles = listOf("class 6th chapter 1.pdf", "class 6th chapter 2.pdf")

    // State variables (remember{} keeps them alive across recompositions)
    var selectedPdf by remember { mutableStateOf(pdfFiles[0]) } // currently selected PDF
    var expanded by remember { mutableStateOf(false) } // dropdown expanded/collapsed
    var extractedText by remember { mutableStateOf("No text yet") } // extracted text
    var isLoading by remember { mutableStateOf(false) } // loading state for button

    // Coroutine scope tied to the composable (cancels when composable leaves screen)
    val scope = rememberCoroutineScope()

    // Main vertical layout
    Column(modifier = Modifier.padding(16.dp)) {

        // ---------- PDF Selection Dropdown ----------
        ExposedDropdownMenuBox(
            expanded = expanded, // whether the dropdown is visible
            onExpandedChange = { expanded = !expanded } // toggle dropdown
        ) {
            // Text field showing currently selected PDF
            TextField(
                value = selectedPdf,
                onValueChange = {}, // read-only field (user cannot type)
                readOnly = true,
                label = { Text("Select PDF") },
                modifier = Modifier.menuAnchor().fillMaxWidth() // anchor for dropdown
            )

            // Dropdown menu itself
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false } // close when user clicks outside
            ) {
                pdfFiles.forEach { pdf ->
                    DropdownMenuItem(
                        text = { Text(pdf) }, // show each filename
                        onClick = {
                            selectedPdf = pdf // update selected PDF
                            expanded = false // close menu
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // vertical spacing

        // ---------- Extract & Read Button ----------
        Button(
            onClick = {
                // Launch a coroutine (so heavy work is off the UI thread)
                scope.launch {
                    isLoading = true // show loading state
                    // Switch to background thread (IO dispatcher) for PDF parsing
                    val result = withContext(Dispatchers.IO) {
                        extractTextFromPdf(context, selectedPdf, 1)
                    }
                    // Join characters together to form plain text
                    extractedText = result.joinToString("") { it.first }
                    isLoading = false // loading finished

                    // Use TTS to read the extracted text aloud
                    tts.speak(extractedText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // disable button while loading
        ) {
            // Button text changes depending on state
            Text(if (isLoading) "Extracting..." else "Extract & Read Page 1")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Display Extracted Text ----------
        Text("Extracted Text:\n$extractedText")
    }
}

// Function to extract text + positions from a PDF file in assets
fun extractTextFromPdf(
    context: Context,
    assetFileName: String,
    pageNumber: Int
): List<Pair<String, TextPosition>> {
    // Copy the asset file to cache directory (PDFBox requires a real file, not asset stream)
    val file = File(context.cacheDir, assetFileName)
    if (!file.exists()) {
        // Copy file from assets to cache
        context.assets.open(assetFileName).use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }Frog Jump | Khandaani Tareeka | Memoization | GOOGLE | AMAZON | META | Leetcode-403
codestorywithMIK
17K views • 2 years ago
18:19
Interleaving Strings - Dynamic Programming - Leetcode 97 - Python
NeetCode
130K views • 4 years ago
9:23
Why Experienced Female Employee Shocking Forced Resign TCS PUNE 😱
Corporate Skills
1.6K views • 3 days ago
New
Auto-dubbed
23:35
Interleaving String #leetcode #dynamicprogramming
Deepti Talesra
1K views • 7 months ago
123 lessons
Dynamic Programming : Popular Interview Problems (Explanation + Solution)
codestorywithMIK • Course

    // Load PDF into memory
    val document = PDDocument.load(file)

    // Our custom stripper that records character positions
    val stripper = PositionTextStripper()
    stripper.startPage = pageNumber // extract from this page
    stripper.endPage = pageNumber   // to this page
    stripper.getText(document)      // triggers extraction

    // Close PDF after use (to free memory)
    document.close()

    // Return list of (character, position info)
    return stripper.textWithPositions
}
