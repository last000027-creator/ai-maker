package com.eva.evolutionaryai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eva.evolutionaryai.ui.EvaViewModel
import com.eva.evolutionaryai.ui.MainScreen
import com.eva.evolutionaryai.ui.SettingsScreen
import com.eva.evolutionaryai.ui.theme.EvaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EvaViewModel by viewModels()

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Whatever the real result is (granted or denied), reflect it honestly.
        viewModel.refreshDeviceCapabilities()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.uiState.value.hasMicPermission) {
            requestMicPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            EvaTheme {
                var showSettings by remember { mutableStateOf(false) }
                val state by viewModel.uiState.collectAsState()

                if (showSettings) {
                    val cfg = viewModel.getHttpProviderConfig()
                    SettingsScreen(
                        providers = state.availableProviders,
                        activeProviderId = state.activeProviderId,
                        initialBaseUrl = cfg.baseUrl,
                        initialApiKey = cfg.apiKey,
                        initialModel = cfg.model,
                        onSelectProvider = viewModel::selectProvider,
                        onSaveHttpConfig = viewModel::saveHttpProviderConfig,
                        onBack = { showSettings = false }
                    )
                } else {
                    MainScreen(
                        state = state,
                        onMicPressed = viewModel::onMicPressed,
                        onSendPressed = viewModel::onSendPressed,
                        onInputChanged = viewModel::onInputTextChanged,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDeviceCapabilities()
    }
}
