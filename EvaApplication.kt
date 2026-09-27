package com.eva.evolutionaryai

import android.app.Application
import com.eva.evolutionaryai.ai.AIProvider
import com.eva.evolutionaryai.ai.providers.HttpAIProvider
import com.eva.evolutionaryai.ai.providers.LocalEchoProvider
import com.eva.evolutionaryai.brain.EvaBrain
import com.eva.evolutionaryai.settings.SettingsManager

class EvaApplication : Application() {

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var providers: Map<String, AIProvider>
        private set

    lateinit var brain: EvaBrain
        private set

    override fun onCreate() {
        super.onCreate()

        settingsManager = SettingsManager(this)

        val localProvider = LocalEchoProvider()
        val httpProvider = HttpAIProvider(config = { settingsManager.getHttpProviderConfig() })

        providers = listOf(localProvider, httpProvider).associateBy { it.id }

        brain = EvaBrain(activeProvider = {
            providers[settingsManager.getActiveProviderId()] ?: localProvider
        })
    }
}
