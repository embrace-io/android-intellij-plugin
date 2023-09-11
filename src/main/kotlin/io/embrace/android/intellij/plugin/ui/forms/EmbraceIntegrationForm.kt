package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBScrollPane
import io.embrace.android.intellij.plugin.data.StartMethodStatus
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.OnboardConnectionCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.VerifyIntegrationCallback
import io.embrace.android.intellij.plugin.repository.sentry.SentryLogger
import io.embrace.android.intellij.plugin.services.TrackingEvent
import io.embrace.android.intellij.plugin.services.TrackingService
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbClickableUnderlinedLabel
import io.embrace.android.intellij.plugin.ui.components.EmbLabel
import io.embrace.android.intellij.plugin.ui.components.EmbTextArea
import io.embrace.android.intellij.plugin.ui.components.FormComponentManager
import io.embrace.android.intellij.plugin.ui.components.IntegrationStep
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.ui.constants.Colors
import io.embrace.android.intellij.plugin.utils.extensions.text
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingUtilities


private const val VERTICAL_SPACE = 20
internal const val VERTICAL_SPACE_SMALL = 10
internal const val HORIZONTAL_SPACE = 20
internal const val RIGHT_MARGIN = 50


internal class EmbraceIntegrationForm(
    private val project: Project,
    private val dataProvider: EmbraceIntegrationDataProvider,
) : ConfigFileCreationCallback, ProjectGradleFileModificationCallback, StartMethodCallback, OnboardConnectionCallback,
    VerifyIntegrationCallback {

    internal val panel = object: JPanel() {
        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(VERTICAL_SPACE_SMALL, HORIZONTAL_SPACE, VERTICAL_SPACE, 0)
            background = Colors.panelBackground
        }

        // override the maximum size to prevent stretching
        override fun getMaximumSize(): Dimension {
            return Dimension(preferredSize.width, super.getMaximumSize().height)
        }
    }

    private val scrollPane = JBScrollPane(panel)
    private val componentManager = FormComponentManager(panel)
    private val trackingService = service<TrackingService>()

    private var gradlePopup: GradleFilesPopup? = null
    private val btnOpenDashboard =
        EmbButton("btnOpenDashboard".text(), IntegrationStep.VERIFY_INTEGRATION) { dataProvider.openDashboard() }

    init {
        SwingUtilities.invokeLater {
            initGetStartedLayout()
            initCreateAppStep()
            initConfigFileStep()
            initDependenciesStep()
            initStartEmbraceStep()
            initEmbraceVerificationStep()
            addSupportContact()

            componentManager.setCurrentStep(IntegrationStep.CREATE_PROJECT)

            // This will make sure the scrollPane view is at the top when it is first shown
            SwingUtilities.invokeLater {
                scrollPane.viewport.viewPosition = Point(0, 0)
            }
        }
    }

    fun getContent(): JBScrollPane {
        return scrollPane
    }

    private fun initGetStartedLayout() {
        panel.add(EmbTextArea("getStartedTitle".text(), TextStyle.HEADLINE_1, step = IntegrationStep.CREATE_PROJECT))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("getStartedDescription".text(), TextStyle.BODY, step = IntegrationStep.CREATE_PROJECT))
        panel.add(EmbLabel("syncProject".text(), TextStyle.BODY, step = IntegrationStep.CREATE_PROJECT).apply {
            preferredSize = Dimension(panel.preferredSize.width, panel.preferredSize.height)
        })
//        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        val separator = JSeparator().apply {
            maximumSize = Dimension(Int.MAX_VALUE, 1)
            background = Colors.grayBackground
        }

        panel.add(separator)
    }

    private fun initCreateAppStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step1Title".text(), TextStyle.HEADLINE_2, step = IntegrationStep.CREATE_PROJECT))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step1Description".text(), TextStyle.BODY, step = IntegrationStep.CREATE_PROJECT))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnConnect".text(), IntegrationStep.CREATE_PROJECT) {
            dataProvider.connectToEmbrace(this)
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.connectEmbraceResultPanel)
    }

    private fun initConfigFileStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step2Title".text(), TextStyle.HEADLINE_2, step = IntegrationStep.CONFIG_FILE_CREATION))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("createConfigFile".text(), TextStyle.BODY, step = IntegrationStep.CONFIG_FILE_CREATION))

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.configFieldsLayout)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))

        panel.add(EmbButton("btnConfigFile".text(), IntegrationStep.CONFIG_FILE_CREATION) {
            if (dataProvider.validateConfigFields(componentManager.getAppId(), componentManager.getToken())) {
                dataProvider.createConfigurationEmbraceFile(
                    componentManager.getAppId(), componentManager.getToken(), this
                )
            } else {
                componentManager.changeResultText(
                    componentManager.configFileStatusPanel,
                    "noIdOrTokenError".text(),
                    success = false,
                    displaySkip = false
                )
            }
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.configFileStatusPanel)
    }

    private fun initDependenciesStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step3Title".text(), TextStyle.HEADLINE_2, step = IntegrationStep.DEPENDENCY_UPDATE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("applySwazzlerPlugin".text(), TextStyle.BODY, step = IntegrationStep.DEPENDENCY_UPDATE))

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbButton("btnModifyGradleFiles".text(), IntegrationStep.DEPENDENCY_UPDATE) {
            showGradlePopup()
        })
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.gradleResultPanel)

        componentManager.addDependenciesExplanation(dataProvider.getSwazzlerExampleCode(), dataProvider.getSwazzlerPluginExampleCode())
        componentManager.addExtraModulesExplanation(dataProvider.getSdkExampleCode())
    }

    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step4Title".text(), TextStyle.HEADLINE_2, step = IntegrationStep.START_METHOD_ADDITION))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step4Description".text(), TextStyle.BODY, step = IntegrationStep.START_METHOD_ADDITION))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(dataProvider.getStartExampleCode(), IntegrationStep.START_METHOD_ADDITION))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnAddEmbraceStart".text(), IntegrationStep.START_METHOD_ADDITION) {
            dataProvider.addEmbraceStartMethod(this)
        })
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.startResultPanel)
    }

    private fun initEmbraceVerificationStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step5Title".text(), TextStyle.HEADLINE_2, step = IntegrationStep.VERIFY_INTEGRATION))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step5Description".text(), TextStyle.BODY, step = IntegrationStep.VERIFY_INTEGRATION))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.verifyCheckBox)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        componentManager.btnVerifyIntegration = EmbButton("btnVerifyIntegration".text()) {
            componentManager.verifyResultPanel.isVisible = false
            componentManager.btnVerifyIntegration?.isEnabled = false
            if (dataProvider.verifyIntegration(this, componentManager.getAppId())) {
                componentManager.showLoadingPopup(it)
            }
        }.apply { isEnabled = false }
        panel.add(componentManager.btnVerifyIntegration)
        panel.add(Box.createVerticalStrut(5))
        panel.add(componentManager.verifyResultPanel)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.labelOpenDashboard)
        panel.add(Box.createVerticalStrut(5))
        btnOpenDashboard.isVisible = false
        panel.add(btnOpenDashboard)
    }

    private fun addSupportContact() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        val supportText = "contactInfo".text().replace("{email}", dataProvider.CONTACT_EMAIL)
        panel.add(EmbClickableUnderlinedLabel(supportText) {
            dataProvider.sendSupportEmail()
        }.apply {
            putClientProperty("step", IntegrationStep.CREATE_PROJECT)  // first step so it is always enabled.
        })
        panel.add(Box.createVerticalStrut(5))
        panel.add(EmbLabel("resourcesLink".text(), TextStyle.BODY, step = IntegrationStep.CREATE_PROJECT).apply {
            cursor = Cursor(Cursor.HAND_CURSOR)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    dataProvider.openBrowser()
                }
            })
        })
    }

    override fun onOnboardConnected(appId: String, token: String) {
        componentManager.setAppIdAndToken(appId, token)
        SentryLogger.addAppIdTag(appId)
        SentryLogger.logStepCompleted(IntegrationStep.CREATE_PROJECT)

        componentManager.changeResultText(
            componentManager.connectEmbraceResultPanel, "connectedToEmbraceSuccessfully".text()
        )

        componentManager.setCurrentStep(IntegrationStep.CONFIG_FILE_CREATION)
    }

    override fun onOnboardConnectedError(error: String) {
        componentManager.changeResultText(
            componentManager.connectEmbraceResultPanel,
            "connectedToEmbraceError".text(),
            success = false,
            displaySkip = true
        )
    }

    override fun onConfigSuccess() {
        componentManager.changeResultText(
            componentManager.configFileStatusPanel, "configFileCreated".text()
        )

        componentManager.setCurrentStep(IntegrationStep.DEPENDENCY_UPDATE)
    }

    override fun onConfigAlreadyExists() {
        val options = arrayOf("Replace", "Cancel")
        val result = Messages.showDialog(
            scrollPane, "replaceConfig".text(), "Replace Configuration", options, 0, Messages.getQuestionIcon()
        )

        if (result == 0) {
            dataProvider.createConfigurationEmbraceFile(
                componentManager.getAppId(), componentManager.getToken(), this, true
            )
        } else {
            componentManager.changeResultText(
                componentManager.configFileStatusPanel, "configFileCreated".text()
            )

            componentManager.setCurrentStep(IntegrationStep.DEPENDENCY_UPDATE)
        }
    }

    override fun onConfigError(error: String) {
        componentManager.changeResultText(
            componentManager.configFileStatusPanel, error, false
        )
    }


    private fun showGradlePopup(isRetry: Boolean = false) {
        if (dataProvider.applicationModules?.isNotEmpty() == true) {
            if (gradlePopup == null) {
                gradlePopup = GradleFilesPopup(
                    dataProvider,
                    dataProvider.applicationModules!!,
                ) { dataProvider.modifyGradleFile(it, this@EmbraceIntegrationForm) }
            }

            if (gradlePopup?.isVisible == false) {
                val ideWindow = WindowManager.getInstance().getIdeFrame(project)?.component
                gradlePopup?.showPopup(ideWindow)
            } else {
                onGradleFileError("noApplicationModule".text())
            }
        } else {
            if (isRetry) {
                onGradleFileError("noApplicationModule".text())
            } else {
                dataProvider.loadApplicationModules()
                showGradlePopup(true)
            }
        }
    }

    override fun onGradleFileError(error: String) {
        componentManager.changeResultText(
            componentManager.gradleResultPanel, "buildFilesErrorShort".text(), false
        )
        Messages.showErrorDialog(scrollPane, error, "GenericErrorTitle".text())
    }

    override fun onGradleFileAlreadyModified() {
        componentManager.changeResultText(
            componentManager.gradleResultPanel, "swazzlerPluginAddedResult".text()
        )

        componentManager.setCurrentStep(IntegrationStep.START_METHOD_ADDITION)
        componentManager.showDependenciesExplanation(false)
        componentManager.showExtraModulesExplanation()

        Messages.showInfoMessage(
            scrollPane, "gradleFilesAlreadyAdded".text(), "Info"
        )
    }

    override fun onGradleFilesModifiedSuccessfully() {
        componentManager.changeResultText(
            componentManager.gradleResultPanel, "swazzlerPluginAddedResult".text()
        )

        componentManager.setCurrentStep(IntegrationStep.START_METHOD_ADDITION)
        componentManager.showDependenciesExplanation(false)
        componentManager.showExtraModulesExplanation()

        Messages.showInfoMessage(
            scrollPane, "swazzlerPluginAdded".text(), "Info"
        )
    }

    override fun onStartStatusUpdated(status: StartMethodStatus) {
        when (status) {
            StartMethodStatus.ERROR -> {
                componentManager.changeResultText(
                    componentManager.startResultPanel, "startMethodErrorShort".text(), false
                )
                Messages.showErrorDialog(scrollPane, "startMethodError".text(), "GenericErrorTitle".text())

                trackingService.trackEvent(TrackingEvent.START_SDK_ADDITION_FAILED, buildJsonObject {
                    put("error", "startMethodError".text())
                })
            }

            StartMethodStatus.START_ADDED_SUCCESSFULLY -> {
                componentManager.setCurrentStep(IntegrationStep.VERIFY_INTEGRATION)

                componentManager.changeResultText(
                    componentManager.startResultPanel, "startAddedSuccessfully".text()
                )

                trackingService.trackEvent(TrackingEvent.START_SDK_ADDED)
            }

            StartMethodStatus.START_ALREADY_ADDED -> {
                componentManager.setCurrentStep(IntegrationStep.VERIFY_INTEGRATION)

                componentManager.changeResultText(
                    componentManager.startResultPanel, "startAlreadyAdded".text()
                )

                trackingService.trackEvent(TrackingEvent.START_SDK_ALREADY_ADDED)
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_FOUND -> {
                componentManager.changeResultText(
                    componentManager.startResultPanel, "applicationClassNotFoundShort".text(), false
                )
                Messages.showErrorDialog(scrollPane, "applicationClassNotFound".text(), "GenericErrorTitle".text())

                trackingService.trackEvent(TrackingEvent.START_SDK_ADDITION_FAILED, buildJsonObject {
                    put("error", "applicationClassNotFound".text())
                })
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_ON_CREATE -> {
                componentManager.changeResultText(
                    componentManager.startResultPanel, "applicationClassNotOnCreateShort".text(), false
                )
                Messages.showErrorDialog(
                    scrollPane, "applicationClassNotOnCreate".text(), "GenericErrorTitle".text()
                )

                trackingService.trackEvent(TrackingEvent.START_SDK_ADDITION_FAILED, buildJsonObject {
                    put("error", "applicationClassNotOnCreate".text())
                })
            }

        }
    }


    override fun onEmbraceIntegrationSuccess() {
        componentManager.btnVerifyIntegration?.isEnabled = true
        componentManager.hideLoadingPopup()
        btnOpenDashboard.isVisible = true
        componentManager.labelOpenDashboard.isVisible = true

        componentManager.changeResultText(
            componentManager.verifyResultPanel, "embraceVerificationSuccess".text()
        )

        ApplicationManager.getApplication().invokeLater {
            Messages.showInfoMessage(
                "Embrace is all set! You can now access and review all your sessions in our dashboard.", "Success!"
            )
        }
    }

    override fun onEmbraceIntegrationError() {
        componentManager.btnVerifyIntegration?.isEnabled = true
        componentManager.hideLoadingPopup()
        componentManager.changeResultText(
            componentManager.verifyResultPanel, "embraceVerificationError".text(), success = false, displaySkip = false
        )
    }


}
