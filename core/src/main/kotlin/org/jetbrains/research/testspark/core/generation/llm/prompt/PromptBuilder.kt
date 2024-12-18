package org.jetbrains.research.testspark.core.generation.llm.prompt

import org.jetbrains.research.testspark.core.data.ClassType
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.ClassRepresentation
import org.jetbrains.research.testspark.core.ProjectUnderTestArtifactsCollector

internal class PromptBuilder(private var prompt: String) {
    private fun isPromptValid(
        keyword: PromptKeyword,
        prompt: String,
    ): Boolean {
        val keywordText = keyword.text
        val isMandatory = keyword.mandatory

        return (prompt.contains(keywordText) || !isMandatory)
    }

    private fun hasKeyword(keyword: PromptKeyword, prompt: String): Boolean
        = prompt.contains(keyword.text)

    fun insertLanguage(language: String) = apply {
        if (isPromptValid(PromptKeyword.LANGUAGE, prompt)) {
            val keyword = "\$${PromptKeyword.LANGUAGE.text}"
            prompt = prompt.replace(keyword, language, ignoreCase = false)
        } else {
            throw IllegalStateException("The prompt must contain ${PromptKeyword.LANGUAGE.text}")
        }
    }

    fun insertName(classDisplayName: String) = apply {
        if (isPromptValid(PromptKeyword.NAME, prompt)) {
            val keyword = "\$${PromptKeyword.NAME.text}"
            prompt = prompt.replace(keyword, classDisplayName, ignoreCase = false)
        } else {
            throw IllegalStateException("The prompt must contain ${PromptKeyword.NAME.text}")
        }
    }

    fun insertTestingPlatform(testingPlatformName: String) = apply {
        if (isPromptValid(PromptKeyword.TESTING_PLATFORM, prompt)) {
            val keyword = "\$${PromptKeyword.TESTING_PLATFORM.text}"
            prompt = prompt.replace(keyword, testingPlatformName, ignoreCase = false)
        } else {
            throw IllegalStateException("The prompt must contain ${PromptKeyword.TESTING_PLATFORM.text}")
        }
    }

    fun insertMockingFramework(mockingFrameworkName: String) = apply {
        if (isPromptValid(PromptKeyword.MOCKING_FRAMEWORK, prompt)) {
            val keyword = "\$${PromptKeyword.MOCKING_FRAMEWORK.text}"
            prompt = prompt.replace(keyword, mockingFrameworkName, ignoreCase = false)
        } else {
            throw IllegalStateException("The prompt must contain ${PromptKeyword.MOCKING_FRAMEWORK.text}")
        }
    }

    fun insertCodeUnderTest(text: String, classesToTest: List<ClassRepresentation>) = apply {
        TODO("not implemented")
    }

    fun insertCodeUnderTest(cut: ClassRepresentation, classesToTest: List<ClassRepresentation>, insertSubclasses: Boolean = true) = apply {
        val (keyword, code) = run {
            if (hasKeyword(PromptKeyword.CODE, prompt)) {
                (PromptKeyword.CODE to cut.fullText)
            }
            else if (hasKeyword(PromptKeyword.METHOD_DECLARATIONS, prompt)) {
                (PromptKeyword.METHOD_DECLARATIONS to cut.methodsDeclaration)
            }
            else {
                throw IllegalStateException("The prompt must contain ${PromptKeyword.CODE.text} or ${PromptKeyword.METHOD_DECLARATIONS.text}")
            }
        }

        // buildMethodsDeclarations
        val keywordText = "\$${keyword.text}"
        var fullText = "```\n${code}\n```\n"

        if (insertSubclasses) {
            for (i in 2..classesToTest.size) {
                val subClass = classesToTest[i - 2]
                val superClass = classesToTest[i - 1]

                fullText += "${subClass.qualifiedName} extends ${superClass.qualifiedName}. " +
                        "The source code of ${superClass.qualifiedName} is:\n```\n${superClass.fullText}\n" +
                        "```\n"
            }
        }

        prompt = prompt.replace(keywordText, fullText, ignoreCase = false)
    }

    fun insertMethodsSignatures(interestingClasses: List<ClassRepresentation>) = apply {
        val keyword = "\$${PromptKeyword.METHODS.text}"

        if (isPromptValid(PromptKeyword.METHODS, prompt)) {
            var fullText = ""
            if (interestingClasses.isNotEmpty()) {
                fullText += "Here are some information about other methods and classes used by the class under test. Only use them for creating objects, not your own ideas.\n"
            }
            for (interestingClass in interestingClasses) {
                if (interestingClass.qualifiedName.startsWith("java")) {
                    continue
                }

                fullText += "=== methods in ${interestingClass.qualifiedName}:\n"

                for (method in interestingClass.allMethods) {
                    // Skip java methods
                    // TODO: checks for java methods should be done by a caller to make
                    //       this class as abstract and language agnostic as possible.
                    if (method.containingClassQualifiedName.startsWith("java")) {
                        continue
                    }

                    fullText += " - ${method.signature}\n"
                }
            }
            prompt = prompt.replace(keyword, fullText, ignoreCase = false)
        } else {
            throw IllegalStateException("The prompt must contain ${PromptKeyword.METHODS.text}")
        }
    }

    fun insertPolymorphismRelations(
        polymorphismRelations: Map<ClassRepresentation, List<ClassRepresentation>>,
    ) = apply {
        ProjectUnderTestArtifactsCollector.log("Inserting ${polymorphismRelations.size} polymorphic relations...")

        val keyword = "\$${PromptKeyword.POLYMORPHISM.text}"
        if (isPromptValid(PromptKeyword.POLYMORPHISM, prompt)) {
            var fullText = ""

            polymorphismRelations.forEach { entry ->
                for (currentSubClass in entry.value) {
                    val subClassTypeName = when (currentSubClass.classType) {
                        ClassType.INTERFACE -> "an interface implementing"
                        ClassType.ABSTRACT_CLASS -> "an abstract sub-class of"
                        ClassType.CLASS -> "a sub-class of"
                        ClassType.DATA_CLASS -> "a sub data class of"
                        ClassType.INLINE_VALUE_CLASS -> "a sub inline value class class of"
                        ClassType.OBJECT -> "a sub object of"
                    }
                    fullText += "${currentSubClass.qualifiedName} is $subClassTypeName ${entry.key.qualifiedName}.\n"
                }
            }
            prompt = prompt.replace(keyword, fullText, ignoreCase = false)
        } else {
            throw IllegalStateException("The prompt must contain ${PromptKeyword.POLYMORPHISM.text}")
        }
    }

    fun insertTestSample(testSamplesCode: String) = apply {
        val keyword = "\$${PromptKeyword.TEST_SAMPLE.text}"

        if (isPromptValid(PromptKeyword.TEST_SAMPLE, prompt)) {
            var fullText = testSamplesCode
            if (fullText.isNotBlank()) {
                fullText = "Use this test samples:\n$fullText\n"
            }
            prompt = prompt.replace(keyword, fullText, ignoreCase = false)
        } else {
            throw IllegalStateException("The prompt must contain ${PromptKeyword.TEST_SAMPLE.text}")
        }
    }

    fun build(): String = prompt
}
