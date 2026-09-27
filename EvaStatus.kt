package com.eva.evolutionaryai

/**
 * The single source of truth for what EVA is actually doing right now.
 *
 * Phase 1 rule: nothing sets this to a state that isn't actually true.
 * SPEAKING is only set while TextToSpeech is actually producing audio.
 * LISTENING is only set while SpeechRecognizer is actually active.
 * WORKING is reserved for future phases (build/test/evolution engine);
 * Phase 1 has no long-running actions, so it is unused for now but kept
 * in the enum so later phases don't need to touch every call site that
 * already switches on EvaStatus.
 */
enum class EvaStatus {
    IDLE,
    LISTENING,
    THINKING,
    WORKING,
    SPEAKING,
    ERROR
}
