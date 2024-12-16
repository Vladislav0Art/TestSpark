package org.jetbrains.research.testspark.tools.llm.generation.ollama

import org.jetbrains.research.testspark.core.data.JUnitVersion
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.test.TestsAssembler
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM
import org.jetbrains.research.testspark.core.test.parsers.TestSuiteParser
import org.jetbrains.research.testspark.core.test.parsers.java.JUnitTestSuiteParser
import org.junit.jupiter.api.Test
import org.jetbrains.research.testspark.progress.HeadlessProgressIndicator


class MockTestsAssembler(private val generationData: TestGenerationData) : TestsAssembler() {
    override fun assembleTestSuite(packageName: String): TestSuiteGeneratedByLLM? {
        val junitVersion = JUnitVersion.JUnit4

        val parser = createTestSuiteParser(packageName, junitVersion)
        val testSuite: TestSuiteGeneratedByLLM? = parser.parseTestSuite(super.getContent())

        // save RunWith
        if (testSuite?.runWith?.isNotBlank() == true) {
            generationData.runWith = testSuite.runWith
            generationData.importsCode.add(junitVersion.runWithAnnotationMeta.import)
        } else {
            generationData.runWith = ""
            generationData.importsCode.remove(junitVersion.runWithAnnotationMeta.import)
        }

        // save annotations and pre-set methods
        generationData.otherInfo = testSuite?.otherInfo ?: ""

        return testSuite
    }

    private fun createTestSuiteParser(packageName: String, jUnitVersion: JUnitVersion): TestSuiteParser {
        return JUnitTestSuiteParser(packageName, jUnitVersion)
    }
}


class OllamaRequestManagerTest {

    @Test
    fun send() {
        val model3B = "llama3.2:1b"
        val manager = OllamaRequestManager(model3B)

        val prompt  = """
            Generate unit tests for the following Java class:
            
            ```
            class Calc {
                int sum(int a, int b) {
                    return a + b;
                }
            }
            ```
        """.trimIndent()

        val indicator = HeadlessProgressIndicator()
        val assembler = MockTestsAssembler(TestGenerationData())

        val result = manager.request(
            prompt = prompt,
            indicator = indicator,
            packageName = "test.org",
            testsAssembler = assembler,
        )

        println("Request result: $result")
        println("Response: '''\n${assembler.getContent()}\n'''")
    }
}