package com.example.talktile_05.services

import android.os.Bundle
import android.speech.RecognitionListener
import android.util.Log

class SimpleRecognitionListener(
    private val onFinal: (String) -> Unit,
    private val onFailure: (String) -> Unit
) : RecognitionListener {

    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        val message = when (error) {
            1 -> "Network error"
            2 -> "Network timeout"
            3 -> "Audio error"
            4 -> "Server error"
            5 -> "Client error"
            6 -> "Speech timeout"
            7 -> "No match"
            8 -> "Recognizer busy"
            else -> "Unknown error $error"
        }

        Log.e("STT", "Speech error: $message")

        // prevent infinite recursion
        onFailure(message)
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?: ""

        onFinal(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
