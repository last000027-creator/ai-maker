package com.eva.evolutionaryai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eva.evolutionaryai.EvaApplication
import com.eva.evolutionaryai.EvaStatus
import com.eva.evolutionaryai.ai.AIProvider
import com.eva.evolutionaryai.brain.BrainResult
import com.eva.evolutionaryai.brain.ConversationMessage
import com.eva.evolutionaryai.brain.Speaker
import com.eva.evolutionaryai.settings.ProviderConfig
import com.eva.evolutionaryai.voice.VoiceEvent
import com.eva.evolutionaryai.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class EvaUiState(
    val status: EvaStatus = EvaStatus.IDLE,
    val conversation: List<ConversationMessage> = emptyList(),
    val inputText: String = "",
    val activeProviderId: String = "local-echo",
    val availableProviders: List<AIProvider> = emptyList(),
    val hasMicPermission: Boolean = false,
    val speechRecognitionAvailable: Boolean = false,
    val statusMessage: String = "Ready."
)

class EvaViewModel(application: Application) : AndroidViewModel(application) {

    private val app: EvaApplication = application as EvaApplication
    private val idGen = AtomicLong(0)

    private val _uiState = MutableStateFlow(
        EvaUiState(
            activeProviderId = app.settingsManager.getActiveProviderId(),
            availableProviders = app.providers.values.toList()
        )
    )
    val uiState: StateFlow<EvaUiState> = _uiState.asStateFlow()

    private val voiceManager = VoiceManager(application) { event -> onVoiceEvent(event) }

    init {
        voiceManager.initTextToSpeech()
        refreshDeviceCapabilities()
        addMessage(
            Speaker.EVA,
            "EVA online — Phase 1 (Foundation). Voice, text-to-speech, and basic " +
                "conversation are active. Actions, memory, and self-upgrade are not " +
                "implemented in this build."
        )
    }

    fun refreshDeviceCapabilities() {
        _uiState.value = _uiState.value.copy(
            hasMicPermission = voiceManager.hasRecordAudioPermission,
            speechRecognitionAvailable = voiceManager.isSpeechRecognitionAvailable
        )
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun onMicPressed() {
        if (_uiState.value.status == EvaStatus.LISTENING) {
            voiceManager.stopListening()
            return
        }
        refreshDeviceCapabilities()
        if (!_uiState.value.hasMicPermission) {
            setStatus(EvaStatus.ERROR, "Microphone permission is required to use voice input.")
            return
        }
        if (!_uiState.value.speechRecognitionAvailable) {
            setStatus(EvaStatus.ERROR, "Speech recognition isn't available on this device.")
            return
        }
        voiceManager.startListening()
    }

    fun onSendPressed() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(inputText = "")
        submitUserText(text)
    }

    fun selectProvider(providerId: String) {
        app.settingsManager.setActiveProviderId(providerId)
        _uiState.value = _uiState.value.copy(activeProviderId = providerId)
    }

    fun saveHttpProviderConfig(baseUrl: String, apiKey: String, model: String) {
        app.settingsManager.saveHttpProviderConfig(baseUrl, apiKey, model)
    }

    fun getHttpProviderConfig(): ProviderConfig = app.settingsManager.getHttpProviderConfig()

    private fun onVoiceEvent(event: VoiceEvent) {
        when (event) {
            is VoiceEvent.ListeningStarted -> setStatus(EvaStatus.LISTENING, "Listening…")
            is VoiceEvent.ListeningStopped -> {
                if (_uiState.value.status == EvaStatus.LISTENING) setStatus(EvaStatus.IDLE, "Ready.")
            }
            is VoiceEvent.RecognizedSpeech -> submitUserText(event.text)
            is VoiceEvent.RecognitionError -> setStatus(EvaStatus.ERROR, event.message)
            is VoiceEvent.SpeakingStarted -> setStatus(EvaStatus.SPEAKING, "Speaking…")
            is VoiceEvent.SpeakingFinished -> {
                if (_uiState.value.status == EvaStatus.SPEAKING) setStatus(EvaStatus.IDLE, "Ready.")
            }
        }
    }

    private fun submitUserText(text: String) {
        addMessage(Speaker.USER, text)
        setStatus(EvaStatus.THINKING, "Thinking…")

        viewModelScope.launch {
            when (val result = app.brain.respond(_uiState.value.conversation)) {
                is BrainResult.Reply -> {
                    addMessage(Speaker.EVA, result.text)
                    setStatus(EvaStatus.IDLE, "Ready.")
                    voiceManager.speak(result.text)
                }
                is BrainResult.ProviderUnavailable -> {
                    addMessage(Speaker.SYSTEM, result.reason)
                    setStatus(EvaStatus.ERROR, result.reason)
                }
            }
        }
    }

    private fun addMessage(speaker: Speaker, text: String) {
        val msg = ConversationMessage(idGen.incrementAndGet(), speaker, text)
        _uiState.value = _uiState.value.copy(conversation = _uiState.value.conversation + msg)
    }

    private fun setStatus(status: EvaStatus, message: String) {
        _uiState.value = _uiState.value.copy(status = status, statusMessage = message)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
