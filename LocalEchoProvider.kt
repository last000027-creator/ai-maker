package com.eva.evolutionaryai.ai.providers

import com.eva.evolutionaryai.ai.AIMessage
import com.eva.evolutionaryai.ai.AIProvider

/**
 * The provider EVA falls back to when no cloud provider is configured, or
 * when the configured one is unreachable. It does NOT pretend to be a full
 * AI — it is a small set of honest, rule-based responses so EVA is never
 * silently stuck and never fabricates an AI answer it didn't actually get.
 */
class LocalEchoProvider : AIProvider {
    override val id: String = "local-echo"
    override val displayName: String = "Local (offline, rule-based)"
    override val isLocal: Boolean = true

    override suspend fun isAvailable(): Boolean = true

    override suspend fun complete(history: List<AIMessage>): String {
        val lastUserText = history.lastOrNull { it.role.name == "USER" }?.content?.trim().orEmpty()
        val lower = lastUserText.lowercase()

        return when {
            lastUserText.isEmpty() ->
                "I didn't catch anything to respond to."

            lower.contains("hello") || lower.contains("hi eva") || lower == "hi" ->
                "Hello. I'm EVA, running in offline mode right now — no cloud AI provider is connected. " +
                    "You can still talk to me, or open Settings to connect a provider."

            lower.contains("what can you do") || lower.contains("what do you do") ->
                "Right now I'm running Phase 1: voice input, text-to-speech, and basic conversation. " +
                    "I don't yet have the ability to open apps, browse, remember things across sessions, " +
                    "or modify my own code — those are later phases and are not implemented yet."

            lower.contains("upgrade yourself") || lower.contains("improve yourself") ||
                lower.contains("fix yourself") || lower.contains("evolve") ->
                "Self-upgrade isn't implemented yet. That's the Evolution Engine, planned for a later phase " +
                    "with a real snapshot → modify → build → test → verify → rollback workflow. " +
                    "I won't pretend to upgrade myself just because you asked."

            lower.startsWith("open ") || lower.contains("search the web") || lower.contains("search for") ->
                "I can't open apps or search the web yet — that's Phase 2 (the Action System), " +
                    "which hasn't been built in this version."

            else ->
                "I heard: \"$lastUserText\". I don't have a cloud AI provider connected, so this is a " +
                    "basic offline reply rather than a full conversational answer. Connect a provider in " +
                    "Settings for real conversational responses."
        }
    }
}
