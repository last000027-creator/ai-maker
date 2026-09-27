package com.eva.evolutionaryai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.eva.evolutionaryai.ai.AIProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    providers: List<AIProvider>,
    activeProviderId: String,
    initialBaseUrl: String,
    initialApiKey: String,
    initialModel: String,
    onSelectProvider: (String) -> Unit,
    onSaveHttpConfig: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("AI Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            providers.forEach { provider ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = activeProviderId == provider.id,
                        onClick = { onSelectProvider(provider.id) }
                    )
                    Text(provider.displayName)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Generic HTTP provider connection (OpenAI-compatible /v1/chat/completions)",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; saved = false },
                label = { Text("Base URL (e.g. https://your-endpoint/v1)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it; saved = false },
                label = { Text("Model name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; saved = false },
                label = { Text("API key (stored encrypted on-device)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                onSaveHttpConfig(baseUrl, apiKey, model)
                saved = true
            }) {
                Text("Save")
            }

            if (saved) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Saved. This key is never hard-coded and never logged; it is stored " +
                        "with EncryptedSharedPreferences (Android Keystore-backed AES-256).",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
