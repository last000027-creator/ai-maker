package com.eva.evolutionaryai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eva.evolutionaryai.EvaStatus
import com.eva.evolutionaryai.brain.ConversationMessage
import com.eva.evolutionaryai.brain.Speaker
import com.eva.evolutionaryai.ui.theme.EvaAccent
import com.eva.evolutionaryai.ui.theme.EvaError
import com.eva.evolutionaryai.ui.theme.EvaSurface
import com.eva.evolutionaryai.ui.theme.EvaTextSecondary
import com.eva.evolutionaryai.ui.theme.EvaWorking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: EvaUiState,
    onMicPressed: () -> Unit,
    onSendPressed: () -> Unit,
    onInputChanged: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EVA", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            StatusBadge(status = state.status, message = state.statusMessage)

            Spacer(Modifier.height(12.dp))

            ConversationList(
                messages = state.conversation,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.height(8.dp))

            InputRow(
                text = state.inputText,
                onTextChanged = onInputChanged,
                onSend = onSendPressed,
                onMic = onMicPressed,
                isListening = state.status == EvaStatus.LISTENING,
                micEnabled = state.hasMicPermission && state.speechRecognitionAvailable
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusBadge(status: EvaStatus, message: String) {
    val color = when (status) {
        EvaStatus.IDLE -> EvaTextSecondary
        EvaStatus.LISTENING -> EvaAccent
        EvaStatus.THINKING -> EvaAccent
        EvaStatus.WORKING -> EvaWorking
        EvaStatus.SPEAKING -> EvaAccent
        EvaStatus.ERROR -> EvaError
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(EvaSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(status.name, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ConversationList(messages: List<ConversationMessage>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(messages, key = { it.id }) { msg ->
            MessageBubble(msg)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MessageBubble(msg: ConversationMessage) {
    val (alignment, bubbleColor, textColor) = when (msg.speaker) {
        Speaker.USER -> Triple(Alignment.CenterEnd, EvaAccent, Color.Black)
        Speaker.EVA -> Triple(Alignment.CenterStart, EvaSurface, Color.White)
        Speaker.SYSTEM -> Triple(Alignment.Center, EvaError.copy(alpha = 0.15f), EvaError)
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Text(
            text = msg.text,
            color = textColor,
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 300.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputRow(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    isListening: Boolean,
    micEnabled: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            onClick = onMic,
            enabled = micEnabled || isListening,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isListening) EvaError else EvaAccent
            )
        ) {
            Icon(
                if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = "Microphone",
                tint = Color.Black
            )
        }

        Spacer(Modifier.width(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Talk to EVA…") },
            singleLine = true
        )

        Spacer(Modifier.width(8.dp))

        FilledIconButton(onClick = onSend) {
            Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.Black)
        }
    }
}
