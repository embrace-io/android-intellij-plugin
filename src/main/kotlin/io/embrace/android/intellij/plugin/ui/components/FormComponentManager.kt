package io.embrace.android.intellij.plugin.ui.components

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import io.embrace.android.intellij.plugin.services.TrackingEvent
import io.embrace.android.intellij.plugin.services.TrackingService
import io.embrace.android.intellij.plugin.ui.constants.Colors
import io.embrace.android.intellij.plugin.ui.constants.Colors.errorColor
import io.embrace.android.intellij.plugin.ui.constants.Colors.successColor
import io.embrace.android.intellij.plugin.ui.forms.VERTICAL_SPACE_SMALL
import io.embrace.android.intellij.plugin.utils.extensions.text
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.awt.Component
import java.awt.Dimension
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
import javax.swing.JSeparator


internal class FormComponentManager(private val mainPanel: JPanel) {
    private val successIcon = IconLoader.getIcon("/icons/check.svg", FormComponentManager::class.java)
    private val errorIcon = IconLoader.getIcon("/icons/error.svg", FormComponentManager::class.java)
    internal val connectEmbraceResultPanel = getResultLayout().apply { isVisible = false }
    private val trackingService = service<TrackingService>()

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
        background = Colors.panelBackground
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

    private fun skipStep() {
        trackingService.trackEvent(TrackingEvent.STEP_SKIPPED, buildJsonObject {
            put("step", currentStep.toString())
        })

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
            add(EmbButton("skipStep".text()) {
                skipStep()
            })
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


    private val projectLevelText =
        EmbTextArea("projectLevelChanges".text(), TextStyle.BODY, step = IntegrationStep.DEPENDENCY_UPDATE)
    private val projectLevelCodeText = EmbBlockCode("", IntegrationStep.DEPENDENCY_UPDATE)

    private val appLevelText =
        EmbTextArea("appLevelChanges".text(), TextStyle.BODY, step = IntegrationStep.DEPENDENCY_UPDATE)
    private val dependenciesChangeText =
        EmbTextArea("dependenciesChange".text(), TextStyle.BODY, step = IntegrationStep.DEPENDENCY_UPDATE)
    private val appLevelCodeText = EmbBlockCode("", IntegrationStep.DEPENDENCY_UPDATE)

    private val space = Box.createVerticalStrut(VERTICAL_SPACE_SMALL)
    private val space1 = Box.createVerticalStrut(VERTICAL_SPACE_SMALL)
    private val space2 = Box.createVerticalStrut(VERTICAL_SPACE_SMALL)
    private val space3 = Box.createVerticalStrut(VERTICAL_SPACE_SMALL)
    private val space4 = Box.createVerticalStrut(VERTICAL_SPACE_SMALL)

    private var isShowingDependenciesExplanation = true
    fun addDependenciesExplanation(projectLevelCode: String, appLevelCode: String) {
        mainPanel.add(space)
        mainPanel.add(dependenciesChangeText)

        mainPanel.add(space1)
        mainPanel.add(projectLevelText)
        mainPanel.add(space2)
        mainPanel.add(projectLevelCodeText.apply { text = projectLevelCode })

        mainPanel.add(space3)
        mainPanel.add(appLevelText)
        mainPanel.add(space4)
        mainPanel.add(appLevelCodeText.apply { text = appLevelCode })
    }

    fun showDependenciesExplanation(shouldShow: Boolean) {
        isShowingDependenciesExplanation = shouldShow
        projectLevelText.isVisible = shouldShow
        projectLevelCodeText.isVisible = shouldShow
        appLevelText.isVisible = shouldShow
        appLevelCodeText.isVisible = shouldShow
        dependenciesChangeText.isVisible = shouldShow
        space.isVisible = shouldShow
        space1.isVisible = shouldShow
        space2.isVisible = shouldShow
        space3.isVisible = shouldShow
        space4.isVisible = shouldShow

        btnSeeChanges.text = if (shouldShow)
            "hideChanges".text()
        else
            "showChanges".text()
    }

    private val space5 = Box.createVerticalStrut(VERTICAL_SPACE_SMALL).apply { isVisible = false }
    private val space6 = Box.createVerticalStrut(VERTICAL_SPACE_SMALL).apply { isVisible = false }
    private val space7 = Box.createVerticalStrut(VERTICAL_SPACE_SMALL).apply { isVisible = false }
    private val extraModuleText =
        EmbLabel(
            "applyDependencyDescription".text(),
            TextStyle.BODY,
            step = IntegrationStep.DEPENDENCY_UPDATE
        ).apply {
            preferredSize = Dimension(mainPanel.preferredSize.width, mainPanel.preferredSize.height)
            isVisible = false
        }

    private val extraModuleCode = EmbBlockCode("", IntegrationStep.DEPENDENCY_UPDATE)
    private val btnSeeChanges =
        EmbClickableUnderlinedLabel("showChanges".text(), IntegrationStep.DEPENDENCY_UPDATE) {
            showDependenciesExplanation(!isShowingDependenciesExplanation)
        }.apply {
            border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
            isVisible = false
        }

    private val separator1 = JSeparator().apply {
        maximumSize = Dimension(Int.MAX_VALUE, 1)
        background = Colors.grayBackground
        isVisible = false
    }
    private val separator2 = JSeparator().apply {
        maximumSize = Dimension(Int.MAX_VALUE, 1)
        background = Colors.grayBackground
        isVisible = false
    }

    fun addExtraModulesExplanation(sdkDependencyCode: String) {
        mainPanel.add(btnSeeChanges)
        mainPanel.add(separator1)
        mainPanel.add(space5)
        mainPanel.add(extraModuleText)
        mainPanel.add(space6)
        mainPanel.add(extraModuleCode.apply {
            text = sdkDependencyCode
            isVisible = false
        })
        mainPanel.add(space7)
        mainPanel.add(separator2)
    }

    fun showExtraModulesExplanation() {
        space5.isVisible = true
        space6.isVisible = true
        space7.isVisible = true
        separator1.isVisible = true
        separator2.isVisible = true
        btnSeeChanges.isVisible = true
        extraModuleText.isVisible = true
        extraModuleCode.isVisible = true
    }
}