package org.jetbrains.research.testspark.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.readText


@Serializable
data class FeedbackCycleIteration(
    val iteration: Int,
    val prompt: String,
    /**
     * LLM response on the current iteration
     */
    val response: String
)

@Serializable
data class IterationsContainer(
    val iterations: MutableList<FeedbackCycleIteration> = mutableListOf()
)

@Serializable
data class TestSuiteCompilationResult(
    val exitCode: Int,
    val compilationMessage: String,
)

@Serializable
data class TestCasesCompilationResult(
    val total: Int,
    val compilable: Int,
)

@Serializable
data class CompilationResult(
    val iteration: Int,
    val testSuite: TestSuiteCompilationResult,
    val testCases: TestCasesCompilationResult,
)

@Serializable
data class CompilationResultsContainer(
    val iterations: MutableList<CompilationResult> = mutableListOf()
)


class ProjectUnderTestArtifactsCollector {
    companion object {
        var projectUnderTestOutputDirectory: String? = null

        fun getOrCreateFileInOutputDirectory(filename: String): Path {
            val filepath = Path.of("${projectUnderTestOutputDirectory!!}/generated-artifacts/$filename")

            // Create the parent directories if they don't exist
            if (!Files.exists(filepath)) {
                val parentDir = filepath.toFile().parentFile
                parentDir.mkdirs()
                filepath.toFile().createNewFile()
            }

            return filepath
        }

        fun appendToFile(content: String, filepath: Path) {
            filepath.writeText(content, options = arrayOf(StandardOpenOption.APPEND))
        }
        /**
         * Appends the given content to a log file into the directory of the project under test.
         *
         * @param content the content to be appended to the log file
         * @param alsoPrint if true then the content will be printed in the console as well
         */
        fun log(content: String, alsoPrint: Boolean = true) {
            val logFilepath = getOrCreateFileInOutputDirectory("test-generation.log")

            appendToFile(content + "\n", logFilepath)
            if (alsoPrint) {
                println(content)
            }
        }

        /**
         * Initializes a JSON file in the project under test directory with an empty [IterationsContainer].
         */
        fun initializeJsonFileWithIterations(filename: String): Path {
            val filepath = getOrCreateFileInOutputDirectory(filename)

            val emptyContainer = IterationsContainer()
            filepath.writeText(Json.encodeToString(emptyContainer))

            return filepath
        }

        fun appendIteration(filepath: Path, iteration: FeedbackCycleIteration) {
            val json = Json { prettyPrint = true }

            // Read existing data
            val container = if (Files.exists(filepath)) {
                val content = filepath.readText()
                Json.decodeFromString(IterationsContainer.serializer(), content)
            } else {
                IterationsContainer()
            }

            // Append new iteration
            container.iterations.add(iteration)

            // Write updated data back to the file
            filepath.writeText(
                json.encodeToString(IterationsContainer.serializer(), container)
            )
        }


        fun initializeJsonFileWithCompilationResults(filename: String): Path {
            val filepath = getOrCreateFileInOutputDirectory(filename)

            val emptyContainer = CompilationResultsContainer()
            filepath.writeText(Json.encodeToString(emptyContainer))

            return filepath
        }

        fun appendCompilationResult(filepath: Path, result: CompilationResult) {
            val json = Json { prettyPrint = true }

            // Read existing data
            val container = if (Files.exists(filepath)) {
                val content = filepath.readText()
                Json.decodeFromString(CompilationResultsContainer.serializer(), content)
            } else {
                CompilationResultsContainer()
            }

            // Append new iteration
            container.iterations.add(result)

            // Write updated data back to the file
            filepath.writeText(
                json.encodeToString(CompilationResultsContainer.serializer(), container)
            )
        }
    }
}