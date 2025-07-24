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
            Log.e("SpeechHandler", "❌ Speech recognition not available on this device.")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        Log.d("SpeechHandler", "🎤 SpeechRecognizer created and ready to listen")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechHandler", "🟢 Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechHandler", "🎙️ Speech input started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Optional: Log.d("SpeechHandler", "RMS: $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SpeechHandler", "🔇 End of speech")
            }

            override fun onError(error: Int) {
                Log.e("SpeechHandler", "❌ Recognition error code: $error")
            }

            override fun onResults(results: Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.lowercase(Locale.getDefault())

                if (spokenText == null) {
                    Log.d("SpeechHandler", "⚠️ No speech recognized.")
                    return
                }

                Log.d("SpeechHandler", "✅ Recognized speech: \"$spokenText\"")
                processCommand(spokenText)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                Log.d("SpeechHandler", "📝 Partial result: $partial")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        Log.d("SpeechHandler", "🔍 Starting speech recognition...")
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        Log.d("SpeechHandler", "🛑 Stopping speech recognition")
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun processCommand(spokenText: String) {
        when {
            "cities" in spokenText -> {
                Log.d("SpeechHandler", "📦 Detected 'cities' keyword → Switching to indianCities.json")
                onCommandDetected("indianCities.json")
            }
            "states" in spokenText -> {
                Log.d("SpeechHandler", "📦 Detected 'states' keyword → Switching to indianStates.json")
                onCommandDetected("indianStates.json")
            }
            else -> {
                Log.d("SpeechHandler", "🔍 No matching keyword in: \"$spokenText\"")
            }
        }
    }
}
