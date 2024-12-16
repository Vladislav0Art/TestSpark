package org.jetbrains.research.testspark.tools.llm.generation

import com.intellij.openapi.project.Project
import org.jetbrains.research.testspark.core.generation.llm.network.RequestManager
import org.jetbrains.research.testspark.services.LLMSettingsService
import org.jetbrains.research.testspark.settings.llm.LLMSettingsState
import org.jetbrains.research.testspark.tools.llm.SettingsArguments
import org.jetbrains.research.testspark.tools.llm.generation.grazie.GrazieRequestManager
import org.jetbrains.research.testspark.tools.llm.generation.ollama.OllamaRequestManager
import org.jetbrains.research.testspark.tools.llm.generation.openai.OpenAIRequestManager

interface RequestManagerFactory {
    fun getRequestManager(project: Project): RequestManager
}

class StandardRequestManagerFactory(private val project: Project) : RequestManagerFactory {
    private val llmSettingsState: LLMSettingsState
        get() = project.getService(LLMSettingsService::class.java).state

    override fun getRequestManager(project: Project): RequestManager {
        return when (val platform = SettingsArguments(project).currentLLMPlatformName()) {
            llmSettingsState.openAIName -> OpenAIRequestManager(project)
            llmSettingsState.grazieName -> GrazieRequestManager(project)
            llmSettingsState.ollamaName -> OllamaRequestManager(project)
            else -> throw IllegalStateException("Unknown selected platform: $platform")
        }
    }
}
