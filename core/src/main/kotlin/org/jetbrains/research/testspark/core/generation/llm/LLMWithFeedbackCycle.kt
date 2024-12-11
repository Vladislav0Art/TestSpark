package org.jetbrains.research.testspark.core.generation.llm

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.research.testspark.core.*
import org.jetbrains.research.testspark.core.data.Report
import org.jetbrains.research.testspark.core.data.TestCase
import org.jetbrains.research.testspark.core.generation.llm.network.LLMResponse
import org.jetbrains.research.testspark.core.generation.llm.network.RequestManager
import org.jetbrains.research.testspark.core.generation.llm.network.ResponseErrorCode
import org.jetbrains.research.testspark.core.generation.llm.prompt.PromptSizeReductionStrategy
import org.jetbrains.research.testspark.core.monitor.DefaultErrorMonitor
import org.jetbrains.research.testspark.core.monitor.ErrorMonitor
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestCompiler
import org.jetbrains.research.testspark.core.test.TestsAssembler
import org.jetbrains.research.testspark.core.test.TestsPersistentStorage
import org.jetbrains.research.testspark.core.test.TestsPresenter
import org.jetbrains.research.testspark.core.test.data.TestCaseGeneratedByLLM
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM
import java.io.File

enum class FeedbackCycleExecutionResult {
    OK,
    NO_COMPILABLE_TEST_CASES_GENERATED,
    CANCELED,
    PROVIDED_PROMPT_TOO_LONG,
    SAVING_TEST_FILES_ISSUE,
}

data class FeedbackResponse(
    val executionResult: FeedbackCycleExecutionResult,
    val generatedTestSuite: TestSuiteGeneratedByLLM?,
    val compilableTestCases: MutableSet<TestCaseGeneratedByLLM>,
) {
    init {
        if (executionResult == FeedbackCycleExecutionResult.OK && generatedTestSuite == null) {
            throw IllegalArgumentException("Test suite must be provided when FeedbackCycleExecutionResult is OK, got null")
        } else if (executionResult != FeedbackCycleExecutionResult.OK && generatedTestSuite != null) {
            throw IllegalArgumentException(
                "Test suite must not be provided when FeedbackCycleExecutionResult is not OK, got $generatedTestSuite",
            )
        }
    }
}

class LLMWithFeedbackCycle(
    private val report: Report,
    private val initialPromptMessage: String,
    private val promptSizeReductionStrategy: PromptSizeReductionStrategy,
    // filename in which the test suite is saved in result path
    private val testSuiteFilename: String,
    private val packageName: String,
    // temp path where all the generated tests and their jacoco report are saved
    private val resultPath: String,
    // all the directories where the compiled code of the project under test is saved. This path will be used as a classpath to run each test case
    private val buildPath: String,
    private val requestManager: RequestManager,
    private val testsAssembler: TestsAssembler,
    private val testCompiler: TestCompiler,
    private val testStorage: TestsPersistentStorage,
    private val testsPresenter: TestsPresenter,
    private val indicator: CustomProgressIndicator,
    private val requestsCountThreshold: Int,
    private val errorMonitor: ErrorMonitor = DefaultErrorMonitor(),
) {
    enum class WarningType {
        TEST_SUITE_PARSING_FAILED,
        NO_TEST_CASES_GENERATED,
        COMPILATION_ERROR_OCCURRED,
    }

    private val log = KotlinLogging.logger { this::class.java }

    fun run(onWarningCallback: ((WarningType) -> Unit)? = null): FeedbackResponse {
        var requestsCount = 0
        var generatedTestsArePassing = false
        var nextPromptMessage = initialPromptMessage

        var executionResult = FeedbackCycleExecutionResult.OK
        val compilableTestCases: MutableSet<TestCaseGeneratedByLLM> = mutableSetOf()

        var generatedTestSuite: TestSuiteGeneratedByLLM? = null


        /**
         * Create files for LLM output and iterations.
         */
        val llmResponseFilepath = ProjectUnderTestArtifactsCollector.getOrCreateFileInOutputDirectory("llm-response.txt")
        val promptsSentFilepath = ProjectUnderTestArtifactsCollector.getOrCreateFileInOutputDirectory("sent-prompts.txt")

        val iterationsJsonFilepath = ProjectUnderTestArtifactsCollector.initializeJsonFileWithIterations("iterations.json")
        val compilationResultsJsonFilepath = ProjectUnderTestArtifactsCollector.initializeJsonFileWithCompilationResults("compilations.json")

        ProjectUnderTestArtifactsCollector.appendToFile(
            "[IMPORTANT]: Max feedback cycle iterations: $requestsCountThreshold\n", llmResponseFilepath)
        ProjectUnderTestArtifactsCollector.log("[IMPORTANT]: Max feedback cycle iterations: $requestsCountThreshold")


        while (!generatedTestsArePassing) {
            requestsCount++


            ProjectUnderTestArtifactsCollector.appendToFile(
                "\n====================== Iteration #$requestsCount ======================\n", llmResponseFilepath)
            ProjectUnderTestArtifactsCollector.appendToFile(
                "\n====================== Iteration #$requestsCount ======================\n", promptsSentFilepath)


            log.info { "Iteration #$requestsCount of feedback cycle" }

            // Process stopped checking
            if (indicator.isCanceled()) {
                executionResult = FeedbackCycleExecutionResult.CANCELED
                break
            }

            if (requestsCount > requestsCountThreshold && compilableTestCases.isEmpty()) {
                executionResult = FeedbackCycleExecutionResult.NO_COMPILABLE_TEST_CASES_GENERATED
                break
            }
            else if (requestsCount > requestsCountThreshold) {
                break
            }

            // clearing test assembler's collected text on the previous attempts
            testsAssembler.clear()
            val response: LLMResponse = requestManager.request(
                prompt = nextPromptMessage,
                indicator = indicator,
                packageName = packageName,
                testsAssembler = testsAssembler,
                isUserFeedback = false,
                errorMonitor,
            )


            val aiRawResponse = testsAssembler.getContent()

            ProjectUnderTestArtifactsCollector.appendToFile(aiRawResponse, llmResponseFilepath)
            ProjectUnderTestArtifactsCollector.appendToFile(
                "\n===================================================\n\n", llmResponseFilepath)

            ProjectUnderTestArtifactsCollector.appendToFile(nextPromptMessage, promptsSentFilepath)
            ProjectUnderTestArtifactsCollector.appendToFile(
                "\n===================================================\n\n", promptsSentFilepath)

            ProjectUnderTestArtifactsCollector.appendIteration(
                filepath = iterationsJsonFilepath,
                iteration = FeedbackCycleIteration(
                    iteration = requestsCount,
                    prompt = nextPromptMessage,
                    promptLength = nextPromptMessage.length,
                    response = aiRawResponse,
                    responseLength = aiRawResponse.length,
                )
            )


            when (response.errorCode) {
                ResponseErrorCode.OK -> {
                    log.info { "Test suite generated successfully:\n${response.testSuite!!}" }
                    // check that there are some test cases generated
                    if (response.testSuite!!.testCases.isEmpty()) {
                        log.info { "Generated test suite is empty. Proceeding to the next iteration..." }
                        onWarningCallback?.invoke(WarningType.NO_TEST_CASES_GENERATED)

                        nextPromptMessage =
                            "You have provided an empty answer! Please answer my previous question with the same formats."
                        continue
                    }
                }
                ResponseErrorCode.PROMPT_TOO_LONG -> {
                    log.info { "Provided prompt exceeds context limit" }

                    if (promptSizeReductionStrategy.isReductionPossible()) {
                        log.info { "Reduction of the prompt length is possible. Attempting with the reduced prompt..." }
                        nextPromptMessage = promptSizeReductionStrategy.reduceSizeAndGeneratePrompt()
                        /**
                         * Current attempt does not count as a failure since it was rejected due to the prompt size exceeding the threshold
                         */
                        requestsCount--
                        continue
                    } else {
                        executionResult = FeedbackCycleExecutionResult.PROVIDED_PROMPT_TOO_LONG
                        break
                    }
                }
                ResponseErrorCode.EMPTY_LLM_RESPONSE -> {
                    log.info { "LLM response is empty. Proceeding to the next iteration..." }
                    nextPromptMessage =
                        "You have provided an empty answer! Please, answer my previous question with the same formats"
                    continue
                }
                ResponseErrorCode.TEST_SUITE_PARSING_FAILURE -> {
                    log.info { "Cannot parse a test suite from the LLM response. LLM response: '$response'. Proceeding to the next iteration..." }
                    onWarningCallback?.invoke(WarningType.TEST_SUITE_PARSING_FAILED)

                    nextPromptMessage = "The provided code is not parsable. Please, generate the correct code"
                    continue
                }
            }

            generatedTestSuite = response.testSuite

            // Process stopped checking
            if (indicator.isCanceled()) {
                executionResult = FeedbackCycleExecutionResult.CANCELED
                break
            }

            // Save the generated TestSuite into a temp file
            val generatedTestCasesPaths: MutableList<String> = mutableListOf()

            if (isLastIteration(requestsCount)) {
                generatedTestSuite.updateTestCases(compilableTestCases.toMutableList())
            }
            else {
                for (testCaseIndex in generatedTestSuite.testCases.indices) {
                    val testCaseFilename = "${getClassWithTestCaseName(generatedTestSuite.testCases[testCaseIndex].name)}.java"

                    val testCaseRepresentation = testsPresenter.representTestCase(generatedTestSuite, testCaseIndex)

                    val saveFilepath = testStorage.saveGeneratedTest(
                        generatedTestSuite.packageString,
                        testCaseRepresentation,
                        resultPath,
                        testCaseFilename,
                    )

                    generatedTestCasesPaths.add(saveFilepath)
                }
            }

            val generatedTestSuitePath: String = testStorage.saveGeneratedTest(
                generatedTestSuite.packageString,
                testsPresenter.representTestSuite(generatedTestSuite),
                resultPath,
                testSuiteFilename,
            )

            // check that the file creation was successful
            var allFilesCreated = true
            for (path in generatedTestCasesPaths) {
                allFilesCreated = allFilesCreated && File(path).exists()
            }
            if (!(allFilesCreated && File(generatedTestSuitePath).exists())) {
                // either some test case file or the test suite file was not created
                log.info { "Couldn't save a test file: '$generatedTestSuitePath'" }
                executionResult = FeedbackCycleExecutionResult.SAVING_TEST_FILES_ISSUE
                break
            }

            // Get test cases
            val testCases: MutableList<TestCaseGeneratedByLLM> = if (!isLastIteration(requestsCount)) {
                    generatedTestSuite.testCases
                } else {
                    compilableTestCases.toMutableList()
                }

            // Compile the test file
            indicator.setText("Compilation tests checking")

            val testCasesCompilationResult = testCompiler.compileTestCases(generatedTestCasesPaths, buildPath, testCases)
            val testSuiteCompilationResult = testCompiler.compileCode(File(generatedTestSuitePath).absolutePath, buildPath)

            // writing JSON file
            ProjectUnderTestArtifactsCollector.appendCompilationResult(
                filepath = compilationResultsJsonFilepath,
                result = CompilationResult(
                    iteration = requestsCount,
                    testSuite = TestSuiteCompilationResult(
                        exitCode = testSuiteCompilationResult.exitCode,
                        compilationMessage = testSuiteCompilationResult.executionMessage,
                    ),
                    testCases = TestCasesCompilationResult(
                        total = generatedTestCasesPaths.size,
                        compilable = testCasesCompilationResult.compilableTestCases.size,
                    ),
                )
            )


            // saving the compilable test cases
            compilableTestCases.addAll(testCasesCompilationResult.compilableTestCases)

            if (!testCasesCompilationResult.allTestCasesCompilable && !isLastIteration(requestsCount)) {
                log.info { "Non-compilable test suite (Proceeding to the next iteration...):\n${testsPresenter.representTestSuite(generatedTestSuite!!)}" }

                onWarningCallback?.invoke(WarningType.COMPILATION_ERROR_OCCURRED)

                nextPromptMessage = """
                    I cannot compile the tests that you provided. The error is:

                    ```
                    ${testSuiteCompilationResult.executionMessage}
                    ```

                    Fix this issue in the provided tests. Generate public classes and public methods.
                    Response only a code with tests between ```, DO NOT provide any other text.
                """.trimIndent()

                log.info { nextPromptMessage }
                continue
            }

            log.info { "Result is compilable" }

            generatedTestsArePassing = true

            for (index in testCases.indices) {
                report.testCaseList[index] = TestCase(index, testCases[index].name, testCases[index].toString(), setOf())
            }
        }

        // test suite must not be provided upon failed execution
        if (executionResult != FeedbackCycleExecutionResult.OK) {
            generatedTestSuite = null
        }

        return FeedbackResponse(
            executionResult = executionResult,
            generatedTestSuite = generatedTestSuite,
            compilableTestCases = compilableTestCases,
        )
    }

    private fun isLastIteration(requestsCount: Int): Boolean = requestsCount == requestsCountThreshold
}
