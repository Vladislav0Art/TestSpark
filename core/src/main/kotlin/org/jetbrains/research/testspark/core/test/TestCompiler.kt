package org.jetbrains.research.testspark.core.test

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.research.testspark.core.test.data.TestCaseGeneratedByLLM
import org.jetbrains.research.testspark.core.utils.CommandLineRunner
import org.jetbrains.research.testspark.core.utils.DataFilesUtil
import java.io.File

data class TestCasesCompilationResult(
    val allTestCasesCompilable: Boolean,
    val compilableTestCases: MutableSet<TestCaseGeneratedByLLM>,
)

data class ExecutionResult(
    val exitCode: Int,
    val executionMessage: String,
) {
    fun isSuccessful(): Boolean = exitCode == 0
}

/**
 * TestCompiler is a class that is responsible for compiling generated test cases using the proper javac.
 * It provides methods for compiling test cases and code files.
 */
open class TestCompiler(
    private val javaHomeDirectoryPath: String,
    private val libPaths: List<String>,
    private val junitLibPaths: List<String>,
) {
    private val log = KotlinLogging.logger { this::class.java }

    /**
     * Compiles the generated files with test cases using the proper javac.
     *
     * @return true if all the provided test cases are successfully compiled,
     *         otherwise returns false.
     */
    fun compileTestCases(
        generatedTestCasesPaths: List<String>,
        buildPath: String,
        testCases: MutableList<TestCaseGeneratedByLLM>,
    ): TestCasesCompilationResult {
        var allTestCasesCompilable = true
        val compilableTestCases: MutableSet<TestCaseGeneratedByLLM> = mutableSetOf()

        for (index in generatedTestCasesPaths.indices) {
            val compilable = compileCode(generatedTestCasesPaths[index], buildPath).isSuccessful()
            allTestCasesCompilable = allTestCasesCompilable && compilable
            if (compilable) {
                compilableTestCases.add(testCases[index])
            }
        }

        return TestCasesCompilationResult(allTestCasesCompilable, compilableTestCases)
    }

    /**
     * Compiles the code at the specified path using the provided project build path.
     *
     * @param path The path of the code file to compile.
     * @param projectBuildPath The project build path to use during compilation.
     * @return A pair containing a boolean value indicating whether the compilation was successful (true) or not (false),
     *         and a string message describing any error encountered during compilation.
     */
    fun compileCode(path: String, projectBuildPath: String): ExecutionResult {
        // find the proper javac
        val javaCompile = File(javaHomeDirectoryPath).walk()
            .filter {
                val isCompilerName = if (DataFilesUtil.isWindows()) it.name.equals("javac.exe") else it.name.equals("javac")
                isCompilerName && it.isFile
            }
            .firstOrNull()

        if (javaCompile == null) {
            val msg = "Cannot find java compiler 'javac' at '$javaHomeDirectoryPath'"
            log.error { msg }
            throw RuntimeException(msg)
        }

        println("javac found at '${javaCompile.absolutePath}'")

        // compile file
        val executionResult = CommandLineRunner.run(
            arrayListOf(
                javaCompile.absolutePath,
                "-cp",
                "\"${getPath(projectBuildPath)}\"",
                path,
            ),
        )

        log.info { "Execution: exitCode=${executionResult.exitCode}, message: '${executionResult.executionMessage}'" }

        // create .class file path
        // val classFilePath = path.replace(".java", ".class")

        // check is .class file exists
        return executionResult
    }

    /**
     * Generates the path for the command by concatenating the necessary paths.
     *
     * @param buildPath The path of the build file.
     * @return The generated path as a string.
     */
    fun getPath(buildPath: String): String {
        // create the path for the command
        val separator = DataFilesUtil.classpathSeparator
        val dependencyLibPath = libPaths.joinToString(separator.toString())
        val junitPath = junitLibPaths.joinToString(separator.toString())

        val path = "$junitPath${separator}$dependencyLibPath${separator}$buildPath".let {
            File(it).normalize().path
        }
        println("[TestCompiler]: The path is: '$path'")

        return path
    }
}
