package com.example.talktile_05.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun VoiceMicButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit
) {
    IconButton(
        onClick = onPress,
        modifier = modifier
            .size(50.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription,
            Modifier.size(30.dp)
        )
    }
}
