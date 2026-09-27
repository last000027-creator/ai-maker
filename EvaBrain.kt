package com.eva.evolutionaryai.brain

import com.eva.evolutionaryai.ai.AIMessage
import com.eva.evolutionaryai.ai.AIProvider
import com.eva.evolutionaryai.ai.AIProviderException
import com.eva.evolutionaryai.ai.AIRole

sealed class BrainResult {
    data class Reply(val text: String) : BrainResult()
    data class ProviderUnavailable(val reason: String) : BrainResult()
}

/**
 * EVA Brain (Phase 1 scope): turns the running conversation into a request
 * to the currently active AIProvider and returns the real result. It never
 * fabricates a reply when the provider fails — it reports ProviderUnavailable
 * with the actual reason instead, per the "AI provider unavailable." rule.
 */
class EvaBrain(private val activeProvider: () -> AIProvider) {

    suspend fun respond(conversation: List<ConversationMessage>): BrainResult {
        val provider = activeProvider()

        if (!provider.isLocal && !provider.isAvailable()) {
            return BrainResult.ProviderUnavailable("AI provider unavailable.")
        }

        val history = conversation
            .filter { it.speaker == Speaker.USER || it.speaker == Speaker.EVA }
            .map { msg ->
                AIMessage(
                    role = if (msg.speaker == Speaker.USER) AIRole.USER else AIRole.ASSISTANT,
                    content = msg.text
                )
            }

        return try {
            BrainResult.Reply(provider.complete(history))
        } catch (e: AIProviderException) {
            BrainResult.ProviderUnavailable(e.message ?: "AI provider unavailable.")
        }
    }
}
