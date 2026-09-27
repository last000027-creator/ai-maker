package com.eva.evolutionaryai.brain

enum class Speaker { USER, EVA, SYSTEM }

/**
 * One line of the visible conversation. SYSTEM is used for honest
 * status/error notices (e.g. "AI provider unavailable.") so the user
 * never confuses a real assistant reply with a fallback notice.
 */
data class ConversationMessage(
    val id: Long,
    val speaker: Speaker,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
