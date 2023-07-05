package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBScrollPane
import io.embrace.android.intellij.plugin.data.AppModule
import io.embrace.android.intellij.plugin.data.StartMethodStatus
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.OnboardConnectionCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.VerifyIntegrationCallback
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbTextArea
import io.embrace.android.intellij.plugin.ui.components.FormComponentManager
import io.embrace.android.intellij.plugin.ui.components.Steps
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.utils.extensions.text
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project
import java.awt.Component
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

import javax.swing.SwingUtilities


private const val VERTICAL_SPACE = 20
private const val VERTICAL_SPACE_SMALL = 10
private const val BORDER_TOP = 0
private const val BORDER_BOTTOM = 20
private const val BORDER_LEFT = 20
private const val BORDER_RIGHT = 30


internal class EmbraceIntegrationForm(
    private val dataProvider: EmbraceIntegrationDataProvider
) : ConfigFileCreationCallback,
    ProjectGradleFileModificationCallback,
    StartMethodCallback,
    OnboardConnectionCallback,
    VerifyIntegrationCallback {

    internal val panel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
    }

    private val scrollPane = JBScrollPane(panel)
    private val componentManager = FormComponentManager()

    private var gradlePopup: GradleFilesPopup? = null
    private val btnOpenDashboard = EmbButton("btnOpenDashboard".text(), Steps.VERIFY) { dataProvider.openDashboard() }

    init {
        SwingUtilities.invokeLater {
            initGetStartedLayout()
            initCreateAppStep()
            initConfigFileStep()
            initDependenciesStep()
            initStartEmbraceStep()
            initEmbraceVerificationStep()

            componentManager.setCurrentStep(panel, Steps.VERIFY)
            scrollToTop()
        }
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
        panel.add(EmbTextArea("createConfigFile".text(), TextStyle.BODY, step = Steps.CONFIG))

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.configFieldsLayout)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnConfigFile".text(), Steps.CONFIG) {
            if (dataProvider.validateConfigFields(componentManager.getAppId(), componentManager.getToken())) {
                dataProvider.createEmbraceFile(componentManager.getAppId(), componentManager.getToken(), this)
            } else {
                componentManager.changeResultText(
                    componentManager.configFileStatusPanel,
                    "noIdOrTokenError".text(),
                    false
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
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerExampleCode(), Steps.GRADLE))

        panel.add(EmbTextArea("applySwazzlerPlugin".text(), TextStyle.BODY, step = Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerPluginExampleCode(), step = Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnModifyGradleFiles".text(), Steps.GRADLE) {
            dataProvider.applicationModules?.let {
                if (it.isNotEmpty()) {
                    showGradlePopupIfNecessary(it)
                } else {
                    Messages.showErrorDialog("noApplicationModule".text(), "GenericErrorTitle".text())
                }
            } ?: Messages.showErrorDialog("noApplicationModule".text(), "GenericErrorTitle".text())
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.gradleResultPanel)
        panel.add(EmbTextArea("applyDependencyDescription".text(), TextStyle.BODY, step = Steps.GRADLE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSdkExampleCode(), Steps.GRADLE))
    }

    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step4Title".text(), TextStyle.HEADLINE_2, step = Steps.ADD_START))
        panel.add(EmbTextArea("step4Description".text(), TextStyle.BODY, step = Steps.ADD_START))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getStartExampleCode(), Steps.ADD_START))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnAddEmbraceStart".text(), Steps.ADD_START) {
            dataProvider.addEmbraceStartMethod(this)
        })
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.startResultPanel)
    }

    private fun initEmbraceVerificationStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step5Title".text(), TextStyle.HEADLINE_2, step = Steps.VERIFY))
        panel.add(EmbTextArea("step5Description".text(), TextStyle.BODY, step = Steps.VERIFY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnVerifyIntegration".text(), Steps.VERIFY) {
            componentManager.showLoadingPopup(it)
            dataProvider.verifyIntegration(this)
        })

        panel.add(Box.createVerticalStrut(5))
        btnOpenDashboard.isVisible = false
        panel.add(btnOpenDashboard)

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("contactInfo".text(), TextStyle.BODY, step = Steps.VERIFY))
    }

    override fun onOnboardConnected(appId: String, token: String) {
        componentManager.setAppIdAndToken(appId, token)

        componentManager.changeResultText(
            componentManager.connectEmbraceResultPanel,
            "connectedToEmbraceSuccessfully".text()
        )

        componentManager.setCurrentStep(panel, Steps.CONFIG)
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

        componentManager.setCurrentStep(panel, Steps.GRADLE)
    }

    override fun onConfigAlreadyExists() {
        val options = arrayOf("Replace", "Cancel")
        val result =
            Messages.showDialog("replaceConfig".text(), "Replace Configuration", options, 0, Messages.getQuestionIcon())

        if (result == 0) {
            dataProvider.createEmbraceFile(componentManager.getAppId(), componentManager.getToken(), this, true)
        }
    }

    override fun onConfigError(error: String) {
        componentManager.changeResultText(
            componentManager.configFileStatusPanel,
            error,
            false
        )
    }


    private fun showGradlePopupIfNecessary(applicationModules: List<AppModule>) {
        if (gradlePopup == null) {
            gradlePopup = GradleFilesPopup(
                dataProvider,
                applicationModules
            ) { dataProvider.modifyGradleFile(it, this@EmbraceIntegrationForm) }
        }

        if (!gradlePopup!!.isVisible) {
            val ideWindow = WindowManager.getInstance().getIdeFrame(project)?.component
            gradlePopup?.showPopup(ideWindow)
        }
    }

    override fun onGradleFileError(error: String) {
        Messages.showInfoMessage(
            error,
            "Error"
        )
    }

    override fun onGradleFileAlreadyModified() {
        componentManager.changeResultText(
            componentManager.gradleResultPanel,
            "swazzlerPluginAddedResult".text()
        )
        Messages.showInfoMessage(
            "gradleFilesAlreadyAdded".text(),
            "Info"
        )

        componentManager.setCurrentStep(panel, Steps.ADD_START)
    }

    override fun onGradleFilesModifiedSuccessfully() {
        componentManager.changeResultText(
            componentManager.gradleResultPanel,
            "swazzlerPluginAddedResult".text()
        )
        Messages.showInfoMessage(
            "swazzlerPluginAdded".text(),
            "Info"
        )

        componentManager.setCurrentStep(panel, Steps.ADD_START)
    }

    override fun onStartStatusUpdated(status: StartMethodStatus) {
        when (status) {
            StartMethodStatus.ERROR -> {
                Messages.showErrorDialog("startMethodError".text(), "GenericErrorTitle".text())
            }

            StartMethodStatus.START_ADDED_SUCCESSFULLY -> {
                componentManager.setCurrentStep(panel, Steps.VERIFY)

                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "startAddedSuccessfully".text()
                )
            }

            StartMethodStatus.START_ALREADY_ADDED -> {
                componentManager.setCurrentStep(panel, Steps.VERIFY)

                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "startAlreadyAdded".text()
                )
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_FOUND -> {
                Messages.showErrorDialog("applicationClassNotFound".text(), "GenericErrorTitle".text())
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_ON_CREATE -> {
                Messages.showErrorDialog("applicationClassNotOnCreate".text(), "GenericErrorTitle".text())
            }

        }
    }


    override fun onEmbraceIntegrationSuccess() {
        componentManager.hideLoadingPopup()
        Messages.showInfoMessage("embraceVerificationSuccess".text(), "Success")
        btnOpenDashboard.isVisible = true
    }

    override fun onEmbraceIntegrationError() {
        componentManager.hideLoadingPopup()
        Messages.showErrorDialog("embraceVerificationError".text(), "GenericErrorTitle".text())
    }


}
