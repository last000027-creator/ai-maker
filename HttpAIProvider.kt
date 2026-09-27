package com.eva.evolutionaryai.ai.providers

import com.eva.evolutionaryai.ai.AIMessage
import com.eva.evolutionaryai.ai.AIProvider
import com.eva.evolutionaryai.ai.AIProviderException
import com.eva.evolutionaryai.settings.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks to any OpenAI-compatible /v1/chat/completions endpoint (works for a
 * self-hosted local model server, an OpenAI-compatible cloud API, or a
 * compatible gateway). Base URL, model name, and API key are all supplied at
 * runtime from ProviderConfig (persisted via EncryptedSharedPreferences) —
 * none of them are hard-coded here.
 *
 * This class does not fabricate a response. If the request fails for any
 * reason, it throws AIProviderException with the real cause; EvaBrain is
 * responsible for surfacing that honestly instead of pretending.
 */
class HttpAIProvider(private val config: () -> ProviderConfig?) : AIProvider {

    override val id: String = "http-generic"
    override val displayName: String = "Generic HTTP (OpenAI-compatible)"
    override val isLocal: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun isAvailable(): Boolean {
        val cfg = config()
        return cfg != null && cfg.baseUrl.isNotBlank() && cfg.apiKey.isNotBlank() && cfg.model.isNotBlank()
    }

    override suspend fun complete(history: List<AIMessage>): String = withContext(Dispatchers.IO) {
        val cfg = config() ?: throw AIProviderException("No AI provider is configured.")
        if (cfg.baseUrl.isBlank() || cfg.apiKey.isBlank() || cfg.model.isBlank()) {
            throw AIProviderException("AI provider configuration is incomplete.")
        }

        val messagesJson = JSONArray()
        for (m in history) {
            messagesJson.put(
                JSONObject()
                    .put("role", m.role.name.lowercase())
                    .put("content", m.content)
            )
        }

        val bodyJson = JSONObject()
            .put("model", cfg.model)
            .put("messages", messagesJson)

        val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${cfg.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw AIProviderException("AI provider returned HTTP ${response.code}: $raw")
                }
                val parsed = JSONObject(raw)
                val choices = parsed.optJSONArray("choices")
                    ?: throw AIProviderException("AI provider response had no 'choices' field.")
                if (choices.length() == 0) {
                    throw AIProviderException("AI provider returned zero choices.")
                }
                val message = choices.getJSONObject(0).optJSONObject("message")
                    ?: throw AIProviderException("AI provider response had no message content.")
                message.optString("content").ifBlank {
                    throw AIProviderException("AI provider returned an empty message.")
                }
            }
        } catch (e: AIProviderException) {
            throw e
        } catch (e: Exception) {
            throw AIProviderException("Failed to reach AI provider: ${e.message}", e)
        }
    }
}
