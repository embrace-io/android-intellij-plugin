package io.embrace.android.intellij.plugin.ui.components

import com.android.tools.idea.wizard.template.Template
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
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
        putClientProperty("step", Steps.CONFIG)
    }

    internal val gradleResultPanel = getResultLayout().apply {
        isVisible = false
        putClientProperty("step", Steps.GRADLE)
    }

    internal val startResultPanel = getResultLayout().apply {
        isVisible = false
        putClientProperty("step", Steps.ADD_START)
    }

    internal var btnVerifyIntegration: EmbButton? = null

    internal val verifyCheckBox = JCheckBox("checkVerify".text()).apply {
        putClientProperty("step", Steps.VERIFY)
        addItemListener {
            btnVerifyIntegration?.isEnabled = it.stateChange == java.awt.event.ItemEvent.SELECTED
        }
    }

    internal val verifyResultPanel = getResultLayout().apply {
        isVisible = false
        putClientProperty("step", Steps.VERIFY)
    }

    internal val labelOpenDashboard =
        EmbTextArea("seeSessions".text(), TextStyle.BODY, step = Steps.VERIFY).apply { isVisible = false }

    private val etAppId = EmbEditableText(step = Steps.CONFIG)
    private val etToken = EmbEditableText(step = Steps.CONFIG)
    private val appIdLabel = EmbLabel("appIdLabel".text(), TextStyle.HEADLINE_3, step = Steps.CONFIG)
    private val tokenLabel = EmbLabel("tokenLabel".text(), TextStyle.HEADLINE_3, step = Steps.CONFIG)
    private var currentStep: Steps = Steps.CREATE_PROJECT
    private val balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(JLabel("Verifying..."))
    private var balloon: Balloon? = null

    internal val configFieldsLayout = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        add(getConfigGridLayout())
        putClientProperty("step", Steps.CONFIG)
    }

    fun setCurrentStep(currentStep: Steps) {
        val enableComponents = mutableListOf<Steps>()
        this.currentStep = currentStep
        when (currentStep) {
            Steps.CREATE_PROJECT -> enableComponents.add(Steps.CREATE_PROJECT)

            Steps.CONFIG -> {
                enableComponents.add(Steps.CREATE_PROJECT)
                enableComponents.add(Steps.CONFIG)
            }

            Steps.GRADLE -> {
                enableComponents.add(Steps.CREATE_PROJECT)
                enableComponents.add(Steps.CONFIG)
                enableComponents.add(Steps.GRADLE)
            }

            Steps.ADD_START -> {
                enableComponents.add(Steps.CREATE_PROJECT)
                enableComponents.add(Steps.CONFIG)
                enableComponents.add(Steps.GRADLE)
                enableComponents.add(Steps.ADD_START)
            }

            Steps.VERIFY -> {
                enableComponents.add(Steps.CREATE_PROJECT)
                enableComponents.add(Steps.CONFIG)
                enableComponents.add(Steps.GRADLE)
                enableComponents.add(Steps.ADD_START)
                enableComponents.add(Steps.VERIFY)
            }
        }
        enableConfigLayout(currentStep != Steps.CREATE_PROJECT)
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
            Steps.CREATE_PROJECT -> {
                setCurrentStep(Steps.CONFIG)
            }

            Steps.CONFIG -> {
                setCurrentStep(Steps.GRADLE)
            }

            Steps.GRADLE -> {
                setCurrentStep(Steps.ADD_START)
            }

            Steps.ADD_START -> {
                setCurrentStep(Steps.VERIFY)
            }

            Steps.VERIFY -> {
                setCurrentStep(Steps.VERIFY)
            }
        }
    }

    private fun getConfigGridLayout(): JPanel {
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
        }

        val panel = JPanel(GridBagLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT

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