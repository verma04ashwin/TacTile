package com.example.tactile_02.helper

import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher

class SpeechHelper(
    private val launcher: ActivityResultLauncher<Intent>,
    private val onResult: (String) -> Unit
) {
    fun startListening() {
        Log.d("SpeechHelper", "Starting speech recognition...")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say map name...")
        }
        launcher.launch(intent)
    }

    fun handleResult(data: Intent?) {
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            Log.d("SpeechHelper", "Speech recognition results: $matches")
            val spokenText = matches[0]
            Log.d("SpeechHelper", "Recognized speech: $spokenText")
            onResult(spokenText)
        } else {
            Log.d("SpeechHelper", "No speech recognized.")
        }
    }
}
