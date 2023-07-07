package io.embrace.android.intellij.plugin.ui.components

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

private const val ICON_SIZE = 20

internal class FormComponentManager {

    private val errorColor = Color.decode("#d42320")
    private val successColor = Color.decode("#75D554")

    private val successIcon = ImageIcon(
        ImageIcon(javaClass.classLoader.getResource("icons/check_small.png")).image.getScaledInstance(
            ICON_SIZE,
            ICON_SIZE,
            Image.SCALE_SMOOTH
        )
    )

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

    internal val labelOpenDashboard = EmbTextArea("seeSessions".text(), TextStyle.BODY).apply { isVisible = false }

    private val etAppId = EmbEditableText(step = Steps.CONFIG)
    private val etToken = EmbEditableText(step = Steps.CONFIG)
    private val appIdLabel = EmbLabel("appIdLabel".text(), TextStyle.HEADLINE_3, step = Steps.CONFIG)
    private val tokenLabel = EmbLabel("tokenLabel".text(), TextStyle.HEADLINE_3, step = Steps.CONFIG)

    private val balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(JLabel("Verifying..."))
    private var balloon: Balloon? = null

    internal val configFieldsLayout = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        add(getGridLayout())
        putClientProperty("step", Steps.CONFIG)
    }

    fun setCurrentStep(parentPanel: JPanel, currentStep: Steps) {
        val enableComponents = mutableListOf<Steps>()

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
                parentPanel.components.forEach { component ->
                    if (component is JComponent) {
                        component.isEnabled = true
                    }
                }
                return
            }
        }
        enableConfigLayout(currentStep != Steps.CREATE_PROJECT)
        parentPanel.components.forEach { component ->
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

    private fun getResultLayout(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentY = Component.CENTER_ALIGNMENT
            alignmentX = Component.LEFT_ALIGNMENT

            add(JLabel(successIcon))
            add(EmbLabel("message", TextStyle.BODY).apply {
                border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
            })
        }
    }

    private fun getGridLayout(): JPanel {
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
            constraints.insets = Insets(0, 10, 0, 0)
            add(etAppId.apply { alignmentX = Component.LEFT_ALIGNMENT }, constraints)

            constraints.weightx = 0.0
            constraints.insets = Insets(5, 0, 0, 0)

            // Second row
            constraints.gridy = 1
            constraints.gridx = 0
            add(tokenLabel, constraints)

            constraints.gridx = 1
            constraints.insets = Insets(5, 10, 0, 0)
            add(etToken, constraints)
        }

        return panel
    }

    fun changeResultText(panel: JPanel, text: String, success: Boolean = true) {
        panel.isVisible = true
        val icon = panel.getComponent(0)
        val label = panel.getComponent(1)

        if (icon is JLabel && label is JLabel) {
            label.text = text
            icon.isVisible = success

            if (success) {
                label.foreground = successColor
            } else {
                label.foreground = errorColor
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