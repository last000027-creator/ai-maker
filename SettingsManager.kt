package com.eva.evolutionaryai.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Which cloud/local provider is active, plus its runtime connection details. */
data class ProviderConfig(
    val providerId: String,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = ""
)

/**
 * Persists provider configuration (including API keys) using
 * EncryptedSharedPreferences (AES-256-GCM, key held in the Android Keystore).
 * The key is never written into source code, never logged, and never
 * included in EVA's conversation history or memory export.
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "eva_secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getActiveProviderId(): String =
        prefs.getString(KEY_ACTIVE_PROVIDER, "local-echo") ?: "local-echo"

    fun setActiveProviderId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER, id).apply()
    }

    fun getHttpProviderConfig(): ProviderConfig = ProviderConfig(
        providerId = "http-generic",
        baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        model = prefs.getString(KEY_MODEL, "") ?: ""
    )

    fun saveHttpProviderConfig(baseUrl: String, apiKey: String, model: String) {
        prefs.edit()
            .putString(KEY_BASE_URL, baseUrl.trim())
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_MODEL, model.trim())
            .apply()
    }

    fun clearHttpProviderConfig() {
        prefs.edit()
            .remove(KEY_BASE_URL)
            .remove(KEY_API_KEY)
            .remove(KEY_MODEL)
            .apply()
    }

    companion object {
        private const val KEY_ACTIVE_PROVIDER = "active_provider_id"
        private const val KEY_BASE_URL = "http_base_url"
        private const val KEY_API_KEY = "http_api_key"
        private const val KEY_MODEL = "http_model"
    }
}
