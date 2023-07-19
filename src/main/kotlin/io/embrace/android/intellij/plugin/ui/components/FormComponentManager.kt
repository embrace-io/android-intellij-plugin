package io.embrace.android.intellij.plugin.ui.components

import com.android.tools.idea.wizard.template.Template
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import io.embrace.android.intellij.plugin.ui.constants.Colors
import io.embrace.android.intellij.plugin.ui.constants.Colors.errorColor
import io.embrace.android.intellij.plugin.ui.constants.Colors.successColor
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


internal class FormComponentManager(private val mainPanel: JPanel) {
    private val successIcon = IconLoader.getIcon("/icons/check.svg", FormComponentManager::class.java)
    private val errorIcon = IconLoader.getIcon("/icons/error.svg", FormComponentManager::class.java)

    internal val connectEmbraceResultPanel = getResultLayout().apply { isVisible = false }

    internal val configFileStatusPanel = getResultLayout().apply {
        isVisible = false
        putClientProperty("step", IntegrationStep.CONFIG_FILE_CREATION)
    }

    internal val gradleResultPanel = getResultLayout().apply {
        isVisible = false
        putClientProperty("step", IntegrationStep.DEPENDENCY_UPDATE)
    }

    internal val startResultPanel = getResultLayout().apply {
        isVisible = false
        putClientProperty("step", IntegrationStep.START_METHOD_ADDITION)
    }

    internal var btnVerifyIntegration: EmbButton? = null

    internal val verifyCheckBox = JCheckBox("checkVerify".text()).apply {
        putClientProperty("step", IntegrationStep.VERIFY_INTEGRATION)
        addItemListener {
            btnVerifyIntegration?.isEnabled = it.stateChange == java.awt.event.ItemEvent.SELECTED
        }
    }

    internal val verifyResultPanel = getResultLayout().apply {
        isVisible = false
        putClientProperty("step", IntegrationStep.VERIFY_INTEGRATION)
    }

    internal val labelOpenDashboard =
        EmbTextArea("seeSessions".text(), TextStyle.BODY, step = IntegrationStep.VERIFY_INTEGRATION).apply {
            isVisible = false
        }

    private val etAppId = EmbEditableText(step = IntegrationStep.CONFIG_FILE_CREATION)
    private val etToken = EmbEditableText(step = IntegrationStep.CONFIG_FILE_CREATION)
    private val appIdLabel =
        EmbLabel("appIdLabel".text(), TextStyle.HEADLINE_3, step = IntegrationStep.CONFIG_FILE_CREATION)
    private val tokenLabel =
        EmbLabel("tokenLabel".text(), TextStyle.HEADLINE_3, step = IntegrationStep.CONFIG_FILE_CREATION)

    private var currentStep: IntegrationStep = IntegrationStep.CREATE_PROJECT
    private val balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(JLabel("Verifying..."))
    private var balloon: Balloon? = null

    internal val configFieldsLayout = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        background = Colors.panelBackground
        add(getConfigGridLayout())
        putClientProperty("step", IntegrationStep.CONFIG_FILE_CREATION)
    }

    fun setCurrentStep(currentStep: IntegrationStep) {
        val enableComponents = mutableListOf<IntegrationStep>()
        this.currentStep = currentStep
        when (currentStep) {
            IntegrationStep.CREATE_PROJECT -> enableComponents.add(IntegrationStep.CREATE_PROJECT)

            IntegrationStep.CONFIG_FILE_CREATION -> {
                enableComponents.add(IntegrationStep.CREATE_PROJECT)
                enableComponents.add(IntegrationStep.CONFIG_FILE_CREATION)
            }

            IntegrationStep.DEPENDENCY_UPDATE -> {
                enableComponents.add(IntegrationStep.CREATE_PROJECT)
                enableComponents.add(IntegrationStep.CONFIG_FILE_CREATION)
                enableComponents.add(IntegrationStep.DEPENDENCY_UPDATE)
            }

            IntegrationStep.START_METHOD_ADDITION -> {
                enableComponents.add(IntegrationStep.CREATE_PROJECT)
                enableComponents.add(IntegrationStep.CONFIG_FILE_CREATION)
                enableComponents.add(IntegrationStep.DEPENDENCY_UPDATE)
                enableComponents.add(IntegrationStep.START_METHOD_ADDITION)
            }

            IntegrationStep.VERIFY_INTEGRATION -> {
                enableComponents.add(IntegrationStep.CREATE_PROJECT)
                enableComponents.add(IntegrationStep.CONFIG_FILE_CREATION)
                enableComponents.add(IntegrationStep.DEPENDENCY_UPDATE)
                enableComponents.add(IntegrationStep.START_METHOD_ADDITION)
                enableComponents.add(IntegrationStep.VERIFY_INTEGRATION)
            }
        }
        enableConfigLayout(currentStep != IntegrationStep.CREATE_PROJECT)
        mainPanel.components.forEach { component ->
            if (component is JComponent) {
                val id = component.getClientProperty("step")
                component.isEnabled = enableComponents.contains(id)
            }
        }
    }

    private fun enableConfigLayout(enable: Boolean) {
        appIdLabel.isEnabled = enable
        etAppId.isEnabled = enable
        tokenLabel.isEnabled = enable
        etToken.isEnabled = enable
    }

    private fun nextStep() {
        when (currentStep) {
            IntegrationStep.CREATE_PROJECT -> {
                setCurrentStep(IntegrationStep.CONFIG_FILE_CREATION)
            }

            IntegrationStep.CONFIG_FILE_CREATION -> {
                setCurrentStep(IntegrationStep.DEPENDENCY_UPDATE)
            }

            IntegrationStep.DEPENDENCY_UPDATE -> {
                setCurrentStep(IntegrationStep.START_METHOD_ADDITION)
            }

            IntegrationStep.START_METHOD_ADDITION -> {
                setCurrentStep(IntegrationStep.VERIFY_INTEGRATION)
            }

            IntegrationStep.VERIFY_INTEGRATION -> {
                setCurrentStep(IntegrationStep.VERIFY_INTEGRATION)
            }
        }
    }

    private fun getConfigGridLayout(): JPanel {
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
        }

        val panel = JPanel(GridBagLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            background = Colors.panelBackground

            // First row
            constraints.gridy = 0
            constraints.gridx = 0
            add(appIdLabel, constraints)

            constraints.gridx = 1
            constraints.insets = JBUI.insetsLeft(10)
            add(etAppId.apply { alignmentX = Component.LEFT_ALIGNMENT }, constraints)

            constraints.weightx = 0.0
            constraints.insets = JBUI.insetsTop(5)

            // Second row
            constraints.gridy = 1
            constraints.gridx = 0
            add(tokenLabel, constraints)

            constraints.gridx = 1
            constraints.insets = JBUI.insets(5, 10, 0, 0)
            add(etToken, constraints)
        }

        return panel
    }

    private fun getResultLayout(): JPanel {
        return JPanel().apply {
            background = Colors.panelBackground
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentY = Component.CENTER_ALIGNMENT
            alignmentX = Component.LEFT_ALIGNMENT

            add(EmbLabel("message", TextStyle.BODY).apply {
                border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
                iconTextGap = 5
            })
            add(Box.createVerticalStrut(8))
            add(EmbClickableUnderlinedLabel("skipStep".text()) {
                nextStep()
            }.apply {
                border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
                foreground = JBColor.GRAY
            }, Template.constraints)
        }
    }

    fun changeResultText(panel: JPanel, text: String, success: Boolean = true, displaySkip: Boolean = true) {
        panel.isVisible = true
        val label = panel.getComponent(0)
        val skipStep = panel.getComponent(2)
        skipStep.isVisible = displaySkip && !success

        if (label is JLabel) {
            label.text = text
            if (success) {
                label.foreground = successColor
                label.icon = successIcon
            } else {
                label.foreground = errorColor
                label.icon = errorIcon
            }
        }
    }

    fun getAppId() = etAppId.text

    fun getToken() = etToken.text

    fun setAppIdAndToken(appId: String, token: String) {
        etAppId.text = appId
        etToken.text = token
    }

    fun showLoadingPopup(component: JComponent) {
        balloon = balloonBuilder.setFillColor(JBColor.background()).setAnimationCycle(500).createBalloon()
        balloon?.show(RelativePoint.getNorthEastOf(component), Balloon.Position.above)
    }

    fun hideLoadingPopup() {
        balloon?.dispose()
    }
}