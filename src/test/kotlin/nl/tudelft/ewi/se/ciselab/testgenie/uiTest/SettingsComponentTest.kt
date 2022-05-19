package nl.tudelft.ewi.se.ciselab.testgenie.uiTest

import com.automation.remarks.junit5.Video
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.JCheckboxFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor
import nl.tudelft.ewi.se.ciselab.testgenie.uiTest.pages.IdeaFrame
import nl.tudelft.ewi.se.ciselab.testgenie.uiTest.pages.SettingsFrame
import nl.tudelft.ewi.se.ciselab.testgenie.uiTest.pages.WelcomeFrame
import nl.tudelft.ewi.se.ciselab.testgenie.uiTest.utils.RemoteRobotExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import kotlin.streams.toList

@TestMethodOrder(OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class SettingsComponentTest {

    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        find(WelcomeFrame::class.java, timeout = Duration.ofSeconds(60)).apply {
            open("untitled")
        }

        Thread.sleep(10000)

        find(IdeaFrame::class.java, timeout = Duration.ofSeconds(60)).apply {
            waitForBackgroundTasks()
            openSettings()
        }
    }

    @Order(1)
    @Test
    @Video
    fun checkTestGenieTabExists(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(60))
        settingsFrame.searchTextBox.text = "TestGenie"

        with(settingsFrame.projectViewTree) {
            waitFor(Duration.ofSeconds(5)) {
                hasText("TestGenie")
            }
            assert(hasText("TestGenie"))
        }
    }

    @Order(2)
    @Test
    @Video
    fun checkTestGenieInSettings(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        val settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(60))
        settingsFrame.findTestGenie()
        assertThat(settingsFrame.introLabel.isShowing).isTrue
        assertThat(settingsFrame.coverageCheckBox.isShowing).isTrue
    }

    @Order(3)
    @Test
    fun changeTestGenieTabValues(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        var settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(15))
        val prevCoverageCheckBoxValue = settingsFrame.coverageCheckBox.isSelected()

        // Change checkbox value and apply the settings
        settingsFrame.coverageCheckBox.setValue(!prevCoverageCheckBoxValue)
        settingsFrame.okSettings()

        // Open settings again
        val ideaFrame = find(IdeaFrame::class.java, timeout = Duration.ofSeconds(15))
        ideaFrame.openSettings()

        // Find again TestGenie
        settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(15))
        settingsFrame.findTestGenie()

        // Change the checkbox to previous state and close settings
        assertThat(settingsFrame.coverageCheckBox.isSelected()).isNotEqualTo(prevCoverageCheckBoxValue)
        settingsFrame.coverageCheckBox.setValue(prevCoverageCheckBoxValue)
        settingsFrame.okSettings()

        // Open settings again
        ideaFrame.openSettings()
    }

    @Order(4)
    @Test
    @Video
    fun checkEvoSuiteTabExists(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(60))
        settingsFrame.searchTextBox.text = "EvoSuite"

        with(settingsFrame.projectViewTree) {
            waitFor(Duration.ofSeconds(5)) {
                hasText("EvoSuite")
            }
            assert(hasText("EvoSuite"))
        }
    }

    @Order(5)
    @Test
    @Video
    fun checkEvoSuiteInSettings(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        val settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(60))
        settingsFrame.findEvoSuite()
        assertThat(settingsFrame.generalSettingsSeparator.isShowing).isTrue
        assertThat(settingsFrame.searchAlgorithmLabel.isShowing).isTrue
        assertThat(settingsFrame.searchAlgorithmComboBox.isShowing).isTrue
        assertThat(settingsFrame.seedLabel.isShowing).isTrue
        assertThat(settingsFrame.seedTextField.isShowing).isTrue
        assertThat(settingsFrame.configurationIdLabel.isShowing).isTrue
        assertThat(settingsFrame.configurationIdField.isShowing).isTrue
        assertThat(settingsFrame.coverageSeparator.isShowing).isTrue

        val generalCheckBoxesList = helperGeneralCheckBoxes(settingsFrame)
        generalCheckBoxesList.forEach { x -> assertThat(x.isShowing).isTrue }

        val coverageCheckBoxesList = helperCoverageCheckBoxes(settingsFrame)
        coverageCheckBoxesList.forEach { x -> assertThat(x.isShowing).isTrue }
    }

    private fun helperCoverageCheckBoxes(settingsFrame : SettingsFrame): List<JCheckboxFixture> {
        return listOf(
            settingsFrame.lineCoverageCheckBox,
            settingsFrame.branchCoverageCheckBox,
            settingsFrame.exceptionCoverageCheckBox,
            settingsFrame.mutationCoverageCheckBox,
            settingsFrame.outputCoverageCheckBox,
            settingsFrame.methodCoverageCheckBox,
            settingsFrame.methodNoExcCoverageCheckBox,
            settingsFrame.cBranchCoverageCheckBox
        )
    }

    @Order(6)
    @Test
    fun changeEvoSuiteTabCoverageSectionValues(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        // Get previous values
        var settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(15))

        var coverageCheckBoxesList = helperCoverageCheckBoxes(settingsFrame)

        val prevCoverageCheckBoxesValueList = coverageCheckBoxesList.stream().map { x -> x.isSelected() }.toList()
        coverageCheckBoxesList.forEach { x -> x.setValue(!x.isSelected()) }

        settingsFrame.okSettings()

        // Open settings again
        val ideaFrame = find(IdeaFrame::class.java, timeout = Duration.ofSeconds(15))
        ideaFrame.openSettings()

        // Find again EvoSuite
        settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(15))
        settingsFrame.findEvoSuite()
        coverageCheckBoxesList = helperCoverageCheckBoxes(settingsFrame)

        // Verify the values changed
        coverageCheckBoxesList.zip(prevCoverageCheckBoxesValueList)
            .forEach { x -> assertThat(x.first.isSelected()).isNotEqualTo(x.second) }

        // Change the checkbox value to previous state and close settings
        coverageCheckBoxesList.forEach { x -> x.setValue(!x.isSelected()) }
        settingsFrame.okSettings()

        // Open settings again
        ideaFrame.openSettings()
    }

    private fun helperGeneralCheckBoxes(settingsFrame : SettingsFrame): List<JCheckboxFixture> {
        return listOf(
            settingsFrame.executeTestsCheckbox,
            settingsFrame.createAssertionsCheckBox,
            settingsFrame.debugModeCheckbox,
            settingsFrame.minimiseTestSuiteCheckBox,
            settingsFrame.flakyTestCheckBox
        )
    }

    @Order(7)
    @Test
    fun changeEvoSuiteTabGeneralSectionValues(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        // Get previous values
        var settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(15))
        var generalCheckBoxesList = helperGeneralCheckBoxes(settingsFrame)
        val prevCoverageCheckBoxesValueList = generalCheckBoxesList.stream().map { x -> x.isSelected() }.toList()

        val prevSearchAlgorithmComboBoxValue = settingsFrame.searchAlgorithmComboBox.selectedText()
        val prevSeedTextFieldValue = settingsFrame.seedTextField.text
        val prevConfigurationIdFieldValue = settingsFrame.configurationIdField.text

        // Change values and apply the settings
        generalCheckBoxesList.forEach { x -> x.setValue(!x.isSelected()) }

        settingsFrame.searchAlgorithmComboBox.selectItem("BREEDER_GA")
        settingsFrame.seedTextField.text = "3"
        settingsFrame.configurationIdField.text = "configuration id here"
        settingsFrame.okSettings()

        // Open settings again
        val ideaFrame = find(IdeaFrame::class.java, timeout = Duration.ofSeconds(15))
        ideaFrame.openSettings()

        // Find again EvoSuite
        settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(15))
        settingsFrame.findEvoSuite()
        generalCheckBoxesList = helperGeneralCheckBoxes(settingsFrame)

        // Verify the values changed
        generalCheckBoxesList.zip(prevCoverageCheckBoxesValueList)
            .forEach { x -> assertThat(x.first.isSelected()).isNotEqualTo(x.second) }

        assertThat(settingsFrame.searchAlgorithmComboBox.selectedText()).isEqualTo("BREEDER_GA")
        assertThat(settingsFrame.seedTextField.text).isEqualTo("3")
        assertThat(settingsFrame.configurationIdField.text).isEqualTo("configuration id here")

        // Change the values to previous state and close settings
        generalCheckBoxesList.forEach { x -> x.setValue(!x.isSelected()) }

        settingsFrame.searchAlgorithmComboBox.selectItem(prevSearchAlgorithmComboBoxValue)
        settingsFrame.configurationIdField.text = prevConfigurationIdFieldValue
        settingsFrame.seedTextField.text = prevSeedTextFieldValue
        settingsFrame.okSettings()

        // Open settings again
        ideaFrame.openSettings()
    }

    @Order(8)
    @Test
    fun testIncorrectSeedDialog(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        val settingsFrame = find(SettingsFrame::class.java, timeout = Duration.ofSeconds(15))
        settingsFrame.seedTextField.text = "text"
        settingsFrame.applySettings()

        val errorDialog: CommonContainerFixture =
            find(settingsFrame.seedDialogLocator, timeout = Duration.ofSeconds(15))

        assertThat(errorDialog.hasText("Incorrect Numeric Type For Seed")).isTrue

        val okDialog =
            errorDialog.button(byXpath("//div[@class='CustomFrameDialogContent'][.//div[@class='Container']]//div[@class='SouthPanel']//div[@class='JButton']"))
        okDialog.click()
    }

    @AfterAll
    fun closeAll(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        find(SettingsFrame::class.java, timeout = Duration.ofSeconds(60)).apply {
            closeSettings()
        }

        find(IdeaFrame::class.java, timeout = Duration.ofSeconds(60)).apply {
            closeProject()
        }
    }
}
