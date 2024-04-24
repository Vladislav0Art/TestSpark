package org.jetbrains.research.testspark.settings.evosuite

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import org.jdesktop.swingx.JXTitledSeparator
import org.jetbrains.research.testspark.bundles.evosuite.EvoSuiteLabelsBundle
import org.jetbrains.research.testspark.bundles.evosuite.EvoSuiteSettingsBundle
import org.jetbrains.research.testspark.data.evosuite.ContentDigestAlgorithm
import org.jetbrains.research.testspark.settings.template.SettingsComponent
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * This class displays and captures changes to the values of the Settings entries.
 */
class EvoSuiteSettingsComponent : SettingsComponent {
    var panel: JPanel? = null

    // Java path
    private var javaPathTextField = JTextField()

    // EvoSuite "input" options (e.g. text, number)
    private var algorithmSelector = ComboBox(ContentDigestAlgorithm.entries.toTypedArray())
    private var configurationIdTextField = JTextField()
    private var seedTextField = JTextField()
    private var evosuitePortField = JTextField()

    // EvoSuite setup checkbox
    private val evosuiteSetupCheckBox: JCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("evosuiteSetupCheckBox"), true)

    // EvoSuite checkboxes options
    private var sandboxCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("sandbox"))
    private var assertionsCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("assertionCreation"))
    private var clientOnThreadCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("debug"))
    private var minimizeCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("minimize"))
    private var junitCheckCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("junitCheck"))

    // Criterion selection checkboxes
    private var criterionSeparator = JXTitledSeparator(EvoSuiteLabelsBundle.defaultValue("criterionSeparator"))
    private var criterionLineCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionLine"))
    private var criterionBranchCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionBranch"))
    private var criterionExceptionCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionException"))
    private var criterionWeakMutationCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionWeakMutation"))
    private var criterionOutputCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionOutput"))
    private var criterionMethodCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionMethod"))
    private var criterionMethodNoExceptionCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionMethodNoExc"))
    private var criterionCBranchCheckBox = JCheckBox(EvoSuiteLabelsBundle.defaultValue("criterionCBranch"))

    var evosuiteSetupCheckBoxSelected: Boolean
        get() = evosuiteSetupCheckBox.isSelected
        set(newStatus) {
            evosuiteSetupCheckBox.isSelected = newStatus
        }

    var javaPath: String
        get() = javaPathTextField.text
        set(newConfig) {
            javaPathTextField.text = newConfig
        }

    var sandbox: Boolean
        get() = sandboxCheckBox.isSelected
        set(newStatus) {
            sandboxCheckBox.isSelected = newStatus
        }

    var assertions: Boolean
        get() = assertionsCheckBox.isSelected
        set(newStatus) {
            assertionsCheckBox.isSelected = newStatus
        }

    var seed: String
        get() = seedTextField.text
        set(newText) {
            seedTextField.text = newText
        }

    var evosuitePort: String
        get() = evosuitePortField.text
        set(newPort) {
            evosuitePortField.text = newPort
        }

    var algorithm: ContentDigestAlgorithm
        get() = algorithmSelector.item
        set(newAlg) {
            algorithmSelector.item = newAlg
        }

    var configurationId: String
        get() = configurationIdTextField.text
        set(newConfig) {
            configurationIdTextField.text = newConfig
        }

    var clientOnThread: Boolean
        get() = clientOnThreadCheckBox.isSelected
        set(newStatus) {
            clientOnThreadCheckBox.isSelected = newStatus
        }

    var junitCheck: Boolean
        get() = junitCheckCheckBox.isSelected
        set(newStatus) {
            junitCheckCheckBox.isSelected = newStatus
        }

    var criterionLine: Boolean
        get() = criterionLineCheckBox.isSelected
        set(newStatus) {
            criterionLineCheckBox.isSelected = newStatus
        }

    var criterionBranch: Boolean
        get() = criterionBranchCheckBox.isSelected
        set(newStatus) {
            criterionBranchCheckBox.isSelected = newStatus
        }

    var criterionException: Boolean
        get() = criterionExceptionCheckBox.isSelected
        set(newStatus) {
            criterionExceptionCheckBox.isSelected = newStatus
        }

    var criterionWeakMutation: Boolean
        get() = criterionWeakMutationCheckBox.isSelected
        set(newStatus) {
            criterionWeakMutationCheckBox.isSelected = newStatus
        }

    var criterionOutput: Boolean
        get() = criterionOutputCheckBox.isSelected
        set(newStatus) {
            criterionOutputCheckBox.isSelected = newStatus
        }

    var criterionMethod: Boolean
        get() = criterionMethodCheckBox.isSelected
        set(newStatus) {
            criterionMethodCheckBox.isSelected = newStatus
        }

    var criterionMethodNoException: Boolean
        get() = criterionMethodNoExceptionCheckBox.isSelected
        set(newStatus) {
            criterionMethodNoExceptionCheckBox.isSelected = newStatus
        }

    var criterionCBranch: Boolean
        get() = criterionCBranchCheckBox.isSelected
        set(newStatus) {
            criterionCBranchCheckBox.isSelected = newStatus
        }

    var minimize: Boolean
        get() = minimizeCheckBox.isSelected
        set(newStatus) {
            minimizeCheckBox.isSelected = newStatus
        }

    init {
        super.initComponent()
    }

    override fun stylizePanel() {
        // Dimensions adjustments
        algorithmSelector.setMinimumAndPreferredWidth(300)

        // Tooltips
        seedTextField.toolTipText = EvoSuiteSettingsBundle.defaultValue("seed")
        evosuitePortField.toolTipText = EvoSuiteSettingsBundle.defaultValue("port")
        configurationIdTextField.toolTipText = EvoSuiteSettingsBundle.defaultValue("configId")
        clientOnThreadCheckBox.toolTipText = EvoSuiteSettingsBundle.defaultValue("debug")
        junitCheckCheckBox.toolTipText = EvoSuiteSettingsBundle.defaultValue("junit")
        criterionSeparator.toolTipText = EvoSuiteSettingsBundle.defaultValue("criterion")
        javaPathTextField.toolTipText = EvoSuiteSettingsBundle.defaultValue("javaPath")
    }

    override fun createSettingsPanel() {
        panel = FormBuilder.createFormBuilder()
            .addComponent(JXTitledSeparator(EvoSuiteLabelsBundle.defaultValue("javaSettings")))
            .addLabeledComponent(JBLabel(EvoSuiteLabelsBundle.defaultValue("javaPath")), javaPathTextField, 10, false)
            .addComponent(JXTitledSeparator(EvoSuiteLabelsBundle.defaultValue("generalSettings")))
            // EvoSuite "input" options (e.g. text, number)
            // Important settings like algorithm selection, seed selection
            .addLabeledComponent(JBLabel(EvoSuiteLabelsBundle.defaultValue("defaultSearch")), algorithmSelector, 10, false)
            .addLabeledComponent(JBLabel(EvoSuiteLabelsBundle.defaultValue("seed")), seedTextField, 10, false)
            .addLabeledComponent(JBLabel(EvoSuiteLabelsBundle.defaultValue("port")), evosuitePortField, 10, false)
            .addLabeledComponent(JBLabel(EvoSuiteLabelsBundle.defaultValue("configId")), configurationIdTextField, 5, false)
            .addComponent(evosuiteSetupCheckBox, 10)
            // Checkboxes settings
            .addComponent(sandboxCheckBox, 10)
            .addComponent(assertionsCheckBox, 10)
            .addComponent(clientOnThreadCheckBox, 10)
            .addComponent(minimizeCheckBox, 10)
            .addComponent(junitCheckCheckBox, 10)
            // Criterion selection checkboxes
            .addComponent(criterionSeparator, 15)
            .addComponent(criterionLineCheckBox, 5)
            .addComponent(criterionBranchCheckBox, 5)
            .addComponent(criterionExceptionCheckBox, 5)
            .addComponent(criterionWeakMutationCheckBox, 5)
            .addComponent(criterionOutputCheckBox, 5)
            .addComponent(criterionMethodCheckBox, 5)
            .addComponent(criterionMethodNoExceptionCheckBox, 5)
            .addComponent(criterionCBranchCheckBox, 5)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun addListeners() {}
}
