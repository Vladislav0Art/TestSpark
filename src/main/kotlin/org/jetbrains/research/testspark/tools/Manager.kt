package org.jetbrains.research.testspark.tools

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.research.testspark.data.Report
import org.jetbrains.research.testspark.editor.Workspace
import org.jetbrains.research.testspark.services.ErrorService
import org.jetbrains.research.testspark.services.RunnerService
import org.jetbrains.research.testspark.tools.evosuite.EvoSuite
import org.jetbrains.research.testspark.tools.llm.Llm
import org.jetbrains.research.testspark.tools.template.Tool

/**
 * Provides methods for generating tests using different tools.
 */
class Manager {
    companion object {
        val tools: List<Tool> = listOf(EvoSuite(), Llm())

        /**
         * Generates tests for a class using EvoSuite.
         *
         * @param e The AnActionEvent object representing the action event.
         */
        fun generateTestsForClassByEvoSuite(e: AnActionEvent) {
            if (e.project!!.service<RunnerService>().isGeneratorRunning()) return

            EvoSuite().generateTestsForClass(e)
            display(e, 1)
        }

        /**
         * Generates tests for a class using Llm tool.
         *
         * @param e the AnActionEvent representing the action event.
         */
        fun generateTestsForClassByLlm(e: AnActionEvent) {
            if (e.project!!.service<RunnerService>().isGeneratorRunning()) return

            Llm().generateTestsForClass(e)
            display(e, 1)
        }

        /**
         * Generates tests for a specific method using EvoSuite.
         *
         * @param e The AnActionEvent object representing the user action.
         */
        fun generateTestsForMethodByEvoSuite(e: AnActionEvent) {
            if (e.project!!.service<RunnerService>().isGeneratorRunning()) return

            EvoSuite().generateTestsForMethod(e)
            display(e, 1)
        }

        /**
         * Generates tests for a given method based on Llm tool.
         *
         * @param e The AnActionEvent object containing information about the action event.
         */
        fun generateTestsForMethodByLlm(e: AnActionEvent) {
            if (e.project!!.service<RunnerService>().isGeneratorRunning()) return

            Llm().generateTestsForMethod(e)
            display(e, 1)
        }

        /**
         * Generates tests for a specific line using EvoSuite tool.
         *
         * @param e AnActionEvent representing the action event.
         */
        fun generateTestsForLineByEvoSuite(e: AnActionEvent) {
            if (e.project!!.service<RunnerService>().isGeneratorRunning()) return

            EvoSuite().generateTestsForLine(e)
            display(e, 1)
        }

        /**
         * Generates tests for a line using Llm tool.
         *
         * @param e The AnActionEvent containing the information about the action
         */
        fun generateTestsForLineByLlm(e: AnActionEvent) {
            if (e.project!!.service<RunnerService>().isGeneratorRunning()) return

            Llm().generateTestsForLine(e)
            display(e, 1)
        }

        /**
         * Displays the given AnActionEvent and the number of used tools.
         *
         * @param e The AnActionEvent to be displayed.
         * @param numberOfUsedTool The number of used tools to be displayed.
         */
        private fun display(e: AnActionEvent, numberOfUsedTool: Int) =
            AppExecutorUtil.getAppScheduledExecutorService().execute(Display(e, numberOfUsedTool))
    }
}

/**
 * A private class that displays the test generation result to the user.
 *
 * @param event The action event that triggered the display.
 * @param numberOfUsedTool The number of test generation tools used.
 */
private class Display(private val event: AnActionEvent, private val numberOfUsedTool: Int) : Runnable {
    private val log = Logger.getInstance(this::class.java)

    override fun run() {
        // waiting time after each iteration
        val sleepDurationMillis: Long = 1000

        // waiting for the generation result
        while (true) {
            // checks if all generator are finished their work
            if (event.project!!.service<Workspace>().testGenerationData.testGenerationResultList.size != numberOfUsedTool) {
                // there is some error during the process running
                if (event.project!!.service<ErrorService>().isErrorOccurred()) break
                log.info("Found ${event.project!!.service<Workspace>().testGenerationData.testGenerationResultList.size} number of results")
                log.info("Waiting for other generation results")
                Thread.sleep(sleepDurationMillis)
                continue
            }

            log.info("Found all $numberOfUsedTool generation results")

            ApplicationManager.getApplication().invokeLater {
                event.project!!.service<Workspace>().receiveGenerationResult(getMergeResult(numberOfUsedTool))
            }

            break
        }

        event.project!!.service<RunnerService>().clear()
    }

    /**
     * Retrieves the merged result of a test generation process.
     *
     * @param numberOfUsedTool The number of tools used for the test generation.
     * @return The merged report containing the results of the test generation process.
     */
    private fun getMergeResult(numberOfUsedTool: Int): Report {
        log.info("Merging $numberOfUsedTool generation results")

        if (numberOfUsedTool == 1) {
            return event.project!!.service<Workspace>().testGenerationData.testGenerationResultList[0]!!
        }
        TODO("implement merge")
    }
}
