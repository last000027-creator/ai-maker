package com.eva.evolutionaryai.ai

/**
 * EVA never hard-codes itself to one AI vendor. Every provider — cloud API,
 * a different vendor's API, or a fully local model added in a later phase —
 * implements this same contract. EvaBrain only ever talks to this interface.
 */
interface AIProvider {
    /** Stable machine id, e.g. "local-echo", "http-generic". Used in settings. */
    val id: String

    /** Human-readable name shown in the Settings screen. */
    val displayName: String

    /** True if this provider needs no network/config (always usable offline). */
    val isLocal: Boolean

    /**
     * Whether this provider is currently usable, given its configuration.
     * Must reflect actual, checkable state (e.g. "is a base URL configured?"),
     * never a hard-coded true.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Send the conversation so far and get EVA's next reply.
     * Implementations must throw AIProviderException on failure rather than
     * returning a fabricated success string.
     */
    suspend fun complete(history: List<AIMessage>): String
}

data class AIMessage(val role: AIRole, val content: String)

enum class AIRole { USER, ASSISTANT, SYSTEM }

class AIProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)
