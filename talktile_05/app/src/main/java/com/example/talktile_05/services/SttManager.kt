package com.example.talktile_05.services

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData

class SttManager {

    private var recognizer: SpeechRecognizer? = null
    val lastResult = MutableLiveData<String>()

    fun startListening(activity: Activity, onResult: (String) -> Unit) {

        // Check device support
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Request RECORD_AUDIO if needed
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
            Toast.makeText(activity, "Please grant microphone permission", Toast.LENGTH_SHORT).show()
            return
        }

        // Fresh recognizer each time
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(activity)

        recognizer?.setRecognitionListener(
            SimpleRecognitionListener(
                onFinal = { text ->
                    lastResult.postValue(text)
                    onResult(text)
                },
                onFailure = { message ->
                    lastResult.postValue("")
                }
            )
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
        }

        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    companion object {
        const val REQUEST_RECORD_AUDIO = 42
    }
}
