package com.example.tactile_main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.*

class SpeechCommandHandler(
    private val activity: Activity,
    private val onCommandDetected: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Log.e("SpeechHandler", "Speech recognition not available on this device.")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(activity)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.lowercase(Locale.getDefault())

                if (spokenText == null) {
                    Log.d("SpeechHandler", "No speech recognized.")
                    return
                }

                Log.d("SpeechHandler", "Heard: $spokenText")
                processCommand(spokenText)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e("SpeechHandler", "Recognition error: $error")
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun processCommand(spokenText: String) {
        when {
            "cities" in spokenText -> onCommandDetected("indianCities.json")
            "states" in spokenText -> onCommandDetected("indianStates.json")
            else -> Log.d("SpeechHandler", "No matching keyword found in: $spokenText")
        }
    }
}
