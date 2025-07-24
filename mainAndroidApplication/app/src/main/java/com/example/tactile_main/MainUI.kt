import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tactile_main.SpeechCommandHandler
import com.example.tactile_main.CameraView

@Composable
fun MainUI(
    speechHandler: SpeechCommandHandler,
    selectedJson: String,
    onJsonSelected: (String) -> Unit
) {
    val options = listOf("indianCities.json", "indianStates.json")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { expanded = true }) {
                Text(text = "Selected: ${selectedJson.removeSuffix(".json")}")
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { file ->
                    DropdownMenuItem(
                        text = { Text(file.removeSuffix(".json")) },
                        onClick = {
                            onJsonSelected(file) // ✅ Update state via callback
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { speechHandler.startListening() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("🎤 Speak Command")
        }

        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(
            factory = { ctx ->
                CameraView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setRegionFile(selectedJson)
                }
            },
            update = { it.setRegionFile(selectedJson) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
