package org.jetbrains.research.testspark.tools.llm.generation.ollama


data class OllamaOptions(
    val temperature: Double
)

data class OllamaChatMessage(
    val role: String,
    val content: String,
)

data class OllamaRequestBody(
    val model: String,
    val messages: List<OllamaChatMessage>,
    val stream: Boolean,
    val options: OllamaOptions? = null,
)