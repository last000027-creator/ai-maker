package com.eva.evolutionaryai.voice

import android.content.Context
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import java.util.Locale

sealed class VoiceEvent {
    data class RecognizedSpeech(val text: String) : VoiceEvent()
    data class RecognitionError(val message: String) : VoiceEvent()
    object ListeningStarted : VoiceEvent()
    object ListeningStopped : VoiceEvent()
    object SpeakingStarted : VoiceEvent()
    object SpeakingFinished : VoiceEvent()
}

/**
 * Wraps Android's real SpeechRecognizer and TextToSpeech. Every callback here
 * corresponds to an actual system callback — there is no simulated listening
 * or simulated speech. Continuous background listening is NOT implemented:
 * Android's SpeechRecognizer only supports a foreground, permission-gated
 * listening session, so EVA is explicit about that limit instead of claiming
 * capabilities it doesn't have.
 */
class VoiceManager(
    private val context: Context,
    private val onEvent: (VoiceEvent) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    val hasRecordAudioPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    val isSpeechRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onEvent(VoiceEvent.SpeakingStarted)
                    }
                    override fun onDone(utteranceId: String?) {
                        onEvent(VoiceEvent.SpeakingFinished)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onEvent(VoiceEvent.SpeakingFinished)
                    }
                })
            }
        }
    }

    fun startListening() {
        if (!hasRecordAudioPermission) {
            onEvent(VoiceEvent.RecognitionError("Microphone permission not granted."))
            return
        }
        if (!isSpeechRecognitionAvailable) {
            onEvent(VoiceEvent.RecognitionError("Speech recognition is not available on this device."))
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    onEvent(VoiceEvent.ListeningStarted)
                }

                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()
                    onEvent(VoiceEvent.ListeningStopped)
                    if (best.isNullOrBlank()) {
                        onEvent(VoiceEvent.RecognitionError("No speech was recognized."))
                    } else {
                        onEvent(VoiceEvent.RecognizedSpeech(best))
                    }
                }

                override fun onError(error: Int) {
                    onEvent(VoiceEvent.ListeningStopped)
                    onEvent(VoiceEvent.RecognitionError(describeError(error)))
                }

                override fun onEndOfSpeech() {
                    onEvent(VoiceEvent.ListeningStopped)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })

            val intent = RecognizerIntent.getVoiceDetailsIntent(context) ?: android.content.Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            startListening(intent)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun speak(text: String) {
        if (!ttsReady) {
            // Honest failure: don't claim to speak if TTS never initialized.
            onEvent(VoiceEvent.SpeakingFinished)
            return
        }
        val utteranceId = "eva_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT -> "Client-side speech recognition error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during speech recognition."
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
        else -> "Unknown speech recognition error (code $error)."
    }
}
