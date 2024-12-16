package org.jetbrains.research.testspark.tools.llm.generation.ollama

import com.google.gson.GsonBuilder
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpRequests.HttpStatusException
import org.jetbrains.research.testspark.bundles.llm.LLMMessagesBundle
import org.jetbrains.research.testspark.core.data.ChatMessage
import org.jetbrains.research.testspark.core.monitor.ErrorMonitor
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestsAssembler
import org.jetbrains.research.testspark.tools.llm.error.LLMErrorManager
import org.jetbrains.research.testspark.tools.llm.generation.IJRequestManager
import java.net.HttpURLConnection
import com.google.gson.Gson
import com.google.gson.JsonParser


class OllamaRequestManager(private val model: String, project: Project) : IJRequestManager(project) {
    private val url = "http://localhost:11434/api/chat"
    private val llmErrorManager = LLMErrorManager()
    private val supportedModels = listOf("llama3.2", "llama3.2:1b")

    init {
        if (model !in supportedModels) {
            throw IllegalStateException("Model $model is not supported. Supported models are: ${supportedModels.joinToString(", ")}")
        }
    }

    override fun send(
        prompt: String,
        indicator: CustomProgressIndicator,
        testsAssembler: TestsAssembler,
        errorMonitor: ErrorMonitor,
    ): SendResult {
        val httpRequest = HttpRequests.post(url, "application/json")

        val messages = buildList {
            chatHistory.forEach {
                val role = when (it.role) {
                    ChatMessage.ChatRole.User -> "user"
                    ChatMessage.ChatRole.Assistant -> "assistant"
                }
                add(OllamaChatMessage(role, it.content))
            }
        }

        val requestBody = OllamaRequestBody(
            model = model,
            messages = messages,
            stream = false,
            options = OllamaOptions(temperature = 0.0)
        )

        var sendResult = SendResult.OK

        try {
            httpRequest.connect {
                it.write(GsonBuilder().create().toJson(requestBody))

                // check response
                when (val responseCode = (it.connection as HttpURLConnection).responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = it.readString()
                        val assistantMessage = parseMessage(response)
                        testsAssembler.consume(assistantMessage.content)
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        llmErrorManager.errorProcess(
                            LLMMessagesBundle.get("serverProblems"),
                            project,
                            errorMonitor,
                        )
                        sendResult = SendResult.OTHER
                    }
                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        llmErrorManager.warningProcess(
                            LLMMessagesBundle.get("tooLongPrompt"),
                            project,
                        )
                        sendResult = SendResult.PROMPT_TOO_LONG
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        llmErrorManager.errorProcess(
                            LLMMessagesBundle.get("wrongToken"),
                            project,
                            errorMonitor,
                        )
                        sendResult = SendResult.OTHER
                    }
                    else -> {
                        llmErrorManager.errorProcess(
                            llmErrorManager.createRequestErrorMessage(responseCode),
                            project,
                            errorMonitor,
                        )
                        sendResult = SendResult.OTHER
                    }
                }
            }
        } catch (e: HttpStatusException) {
            log.info { "Error in sending request: ${e.message}" }
        }

        return sendResult
    }

    private fun parseMessage(jsonString: String): OllamaChatMessage {
        val gson = Gson()
        // Parse the JSON string into a JsonObject
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        // Extract the "message" JsonObject
        val messageJson = jsonObject.getAsJsonObject("message")
        // Deserialize the "message" JsonObject into OllamaChatMessage
        return gson.fromJson(messageJson, OllamaChatMessage::class.java)
    }
}