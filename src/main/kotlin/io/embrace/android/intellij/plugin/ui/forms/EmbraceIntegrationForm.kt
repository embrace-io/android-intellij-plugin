package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.application.ApplicationManager
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
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbClickableUnderlinedLabel
import io.embrace.android.intellij.plugin.ui.components.EmbTextArea
import io.embrace.android.intellij.plugin.ui.components.FormComponentManager
import io.embrace.android.intellij.plugin.ui.components.Steps
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Component
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel


private const val VERTICAL_SPACE = 20
private const val VERTICAL_SPACE_SMALL = 10
private const val HORIZONTAL_SPACE = 20


internal class EmbraceIntegrationForm(
    private val project: Project,
    private val dataProvider: EmbraceIntegrationDataProvider
) : ConfigFileCreationCallback,
    ProjectGradleFileModificationCallback,
    StartMethodCallback,
    OnboardConnectionCallback,
    VerifyIntegrationCallback {

    internal val panel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        border =
            BorderFactory.createEmptyBorder(VERTICAL_SPACE_SMALL, HORIZONTAL_SPACE, VERTICAL_SPACE, HORIZONTAL_SPACE)
    }

    private val scrollPane = JBScrollPane(panel)
    private val componentManager = FormComponentManager(panel)

    private var gradlePopup: GradleFilesPopup? = null
    private val btnOpenDashboard = EmbButton("btnOpenDashboard".text(), Steps.VERIFY) { dataProvider.openDashboard() }

    init {
//        SwingUtilities.invokeLater {
        initGetStartedLayout()
        initCreateAppStep()
        initConfigFileStep()
        initDependenciesStep()
        initStartEmbraceStep()
        initEmbraceVerificationStep()
        addSupportContact()

        componentManager.setCurrentStep(Steps.GRADLE)
        scrollToTop()
//        }
    }

    private fun scrollToTop() {
        // Add HierarchyListener to detect when the view is added to the scroll pane, scroll top and remove it.
        scrollPane.addHierarchyListener(object : HierarchyListener {
            override fun hierarchyChanged(e: HierarchyEvent) {
                if ((e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && scrollPane.isShowing) {
                    scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.minimum
                    scrollPane.horizontalScrollBar.value = scrollPane.horizontalScrollBar.minimum
                    scrollPane.removeHierarchyListener(this)
                }
            }
        })
    }

    fun getContent(): JBScrollPane {
        return scrollPane
    }

    private fun initGetStartedLayout() {
        panel.add(EmbTextArea("getStartedTitle".text(), TextStyle.HEADLINE_1, step = Steps.CREATE_PROJECT))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("getStartedDescription".text(), TextStyle.BODY, step = Steps.CREATE_PROJECT))
    }

    private fun initCreateAppStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step1Title".text(), TextStyle.HEADLINE_2, step = Steps.CREATE_PROJECT))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step1Description".text(), TextStyle.BODY, step = Steps.CREATE_PROJECT))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnConnect".text(), Steps.CREATE_PROJECT) {
            dataProvider.connectToEmbrace(this)
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.connectEmbraceResultPanel)
    }

    private fun initConfigFileStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step2Title".text(), TextStyle.HEADLINE_2, step = Steps.CONFIG))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))

        panel.add(EmbTextArea("createConfigFile".text(), TextStyle.BODY, step = Steps.CONFIG))

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.configFieldsLayout)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))

        panel.add(EmbButton("btnConfigFile".text(), Steps.CONFIG) {
            if (dataProvider.validateConfigFields(componentManager.getAppId(), componentManager.getToken())) {
                dataProvider.createConfigurationEmbraceFile(
                    componentManager.getAppId(),
                    componentManager.getToken(),
                    this
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
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step3Title".text(), TextStyle.HEADLINE_2, step = Steps.GRADLE))
        panel.add(EmbTextArea("addSwazzler".text(), TextStyle.BODY, step = Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(dataProvider.getSwazzlerExampleCode(), Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("applySwazzlerPlugin".text(), TextStyle.BODY, step = Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(dataProvider.getSwazzlerPluginExampleCode(), step = Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnModifyGradleFiles".text(), Steps.GRADLE) {
            showGradlePopup()
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.gradleResultPanel)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("applyDependencyDescription".text(), TextStyle.BODY, step = Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbBlockCode(dataProvider.getSdkExampleCode(), Steps.GRADLE))
    }

    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step4Title".text(), TextStyle.HEADLINE_2, step = Steps.ADD_START))
        panel.add(EmbTextArea("step4Description".text(), TextStyle.BODY, step = Steps.ADD_START))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(dataProvider.getStartExampleCode(), Steps.ADD_START))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnAddEmbraceStart".text(), Steps.ADD_START) {
            dataProvider.addEmbraceStartMethod(this)
        })
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.startResultPanel)
    }

    private fun initEmbraceVerificationStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step5Title".text(), TextStyle.HEADLINE_2, step = Steps.VERIFY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step5Description".text(), TextStyle.BODY, step = Steps.VERIFY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.verifyCheckBox)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        componentManager.btnVerifyIntegration = EmbButton("btnVerifyIntegration".text()) {
            componentManager.verifyResultPanel.isVisible = false
            componentManager.btnVerifyIntegration?.isEnabled = false
            componentManager.showLoadingPopup(it)
            dataProvider.verifyIntegration(this)
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
        panel.add(EmbTextArea("contactInfo".text(), TextStyle.BODY, step = Steps.CREATE_PROJECT))
        panel.add(EmbClickableUnderlinedLabel(dataProvider.CONTACT_EMAIL, isColorHyperlink = true) {
            dataProvider.sendSupportEmail()
        }.apply {
            putClientProperty("step", Steps.CREATE_PROJECT)  // first step so it is always enabled.
        })
    }

    override fun onOnboardConnected(appId: String, token: String) {
        componentManager.setAppIdAndToken(appId, token)

        componentManager.changeResultText(
            componentManager.connectEmbraceResultPanel,
            "connectedToEmbraceSuccessfully".text()
        )

        componentManager.setCurrentStep(Steps.CONFIG)
    }

    override fun onOnboardConnectedError(error: String) {
        componentManager.changeResultText(
            componentManager.connectEmbraceResultPanel,
            "connectedToEmbraceError".text(),
            false
        )
    }

    override fun onConfigSuccess() {
        componentManager.changeResultText(
            componentManager.configFileStatusPanel,
            "configFileCreated".text()
        )

        componentManager.setCurrentStep(Steps.GRADLE)
    }

    override fun onConfigAlreadyExists() {
        val options = arrayOf("Replace", "Cancel")
        val result =
            Messages.showDialog(
                scrollPane,
                "replaceConfig".text(),
                "Replace Configuration",
                options,
                0,
                Messages.getQuestionIcon()
            )

        if (result == 0) {
            dataProvider.createConfigurationEmbraceFile(
                componentManager.getAppId(),
                componentManager.getToken(),
                this,
                true
            )
        } else {
            componentManager.changeResultText(
                componentManager.configFileStatusPanel,
                "configFileCreated".text()
            )

            componentManager.setCurrentStep(Steps.GRADLE)
        }
    }

    override fun onConfigError(error: String) {
        componentManager.changeResultText(
            componentManager.configFileStatusPanel,
            error,
            false
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
            componentManager.gradleResultPanel,
            "buildFilesErrorShort".text(),
            false
        )
        Messages.showErrorDialog(scrollPane, error, "GenericErrorTitle".text())
    }

    override fun onGradleFileAlreadyModified() {
        componentManager.changeResultText(
            componentManager.gradleResultPanel,
            "swazzlerPluginAddedResult".text()
        )
        Messages.showInfoMessage(
            scrollPane,
            "gradleFilesAlreadyAdded".text(),
            "Info"
        )

        componentManager.setCurrentStep(Steps.ADD_START)
    }

    override fun onGradleFilesModifiedSuccessfully() {
        componentManager.changeResultText(
            componentManager.gradleResultPanel,
            "swazzlerPluginAddedResult".text()
        )
        Messages.showInfoMessage(
            scrollPane,
            "swazzlerPluginAdded".text(),
            "Info"
        )

        componentManager.setCurrentStep(Steps.ADD_START)
    }

    override fun onStartStatusUpdated(status: StartMethodStatus) {
        when (status) {
            StartMethodStatus.ERROR -> {
                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "startMethodErrorShort".text(),
                    false
                )
                Messages.showErrorDialog(scrollPane, "startMethodError".text(), "GenericErrorTitle".text())
            }

            StartMethodStatus.START_ADDED_SUCCESSFULLY -> {
                componentManager.setCurrentStep(Steps.VERIFY)

                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "startAddedSuccessfully".text()
                )
            }

            StartMethodStatus.START_ALREADY_ADDED -> {
                componentManager.setCurrentStep(Steps.VERIFY)

                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "startAlreadyAdded".text()
                )
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_FOUND -> {
                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "applicationClassNotFoundShort".text(),
                    false
                )
                Messages.showErrorDialog(scrollPane, "applicationClassNotFound".text(), "GenericErrorTitle".text())
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_ON_CREATE -> {
                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "applicationClassNotOnCreateShort".text(),
                    false
                )
                Messages.showErrorDialog(
                    scrollPane,
                    "applicationClassNotOnCreate".text(),
                    "GenericErrorTitle".text()
                )
            }

        }
    }


    override fun onEmbraceIntegrationSuccess() {
        componentManager.btnVerifyIntegration?.isEnabled = true
        componentManager.hideLoadingPopup()
        btnOpenDashboard.isVisible = true
        componentManager.labelOpenDashboard.isVisible = true

        componentManager.changeResultText(
            componentManager.verifyResultPanel,
            "embraceVerificationSuccess".text()
        )

        ApplicationManager.getApplication().invokeLater {
            Messages.showInfoMessage(
                "Embrace is all set! You can now access and review all your sessions in our dashboard.",
                "Success!"
            )
        }
    }

    override fun onEmbraceIntegrationError() {
        componentManager.btnVerifyIntegration?.isEnabled = true
        componentManager.hideLoadingPopup()
        componentManager.changeResultText(
            componentManager.verifyResultPanel,
            "embraceVerificationError".text(),
            success = false,
            displaySkip = false
        )
    }


}
