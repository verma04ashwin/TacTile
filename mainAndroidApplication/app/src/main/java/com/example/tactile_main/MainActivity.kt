package com.example.tactile_main

import MainUI
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.tactile_main.ui.theme.TacTilemainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Declare state here
            var selectedJson by remember { mutableStateOf("indianCities.json") }

            // Create speech handler that updates selectedJson when a command is recognized
            val speechHandler = remember {
                SpeechCommandHandler(this) { file ->
                    selectedJson = file
                }
            }

            // Composable content
            TacTilemainTheme {
                MainUI(
                    speechHandler = speechHandler,
                    selectedJson = selectedJson,
                    onJsonSelected = { selectedJson = it }
                )
            }

            // Optional: start listening automatically after UI loads
            LaunchedEffect(Unit) {
                speechHandler.startListening()
            }
        }
    }
}
