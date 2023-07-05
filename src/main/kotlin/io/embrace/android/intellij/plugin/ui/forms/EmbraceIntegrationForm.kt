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
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.utils.extensions.text
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project
import java.awt.CardLayout
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

    private val cardLayout = JPanel(CardLayout()).apply {
        border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)

    }
    private val scrollPane = JBScrollPane(cardLayout)
    private val componentManager = FormComponentManager()

    private var gradlePopup: GradleFilesPopup? = null
    private val btnOpenDashboard = EmbButton("btnOpenDashboard".text()) { dataProvider.openDashboard() }

    init {
        SwingUtilities.invokeLater {
            val step1 = initGetStartedLayout()
            val step2 = initConfigFileStep()
            val step3 = initDependenciesStep()
            val step4 = initStartEmbraceStep()
            val step5 = initEmbraceVerificationStep()

            cardLayout.add(step1, "step1")
            cardLayout.add(step2, "step2")
            cardLayout.add(step3, "step3")
            cardLayout.add(step4, "step4")
            cardLayout.add(step5, "step5")
//            initCreateAppStep()
//            initConfigFileStep()
//            initDependenciesStep()
//            initStartEmbraceStep()
//            initEmbraceVerificationStep()
//            scrollToTop()
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

    private fun initGetStartedLayout(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
        }
        panel.add(EmbTextArea("getStartedTitle".text(), TextStyle.HEADLINE_1))
        panel.add(EmbTextArea("getStartedDescription".text(), TextStyle.BODY))

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step1Title".text(), TextStyle.HEADLINE_2))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step1Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnConnect".text()) {
//            dataProvider.connectToEmbrace(this)
            val layout = cardLayout.layout as CardLayout
            layout.next(cardLayout)
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.connectEmbraceResultPanel)
        return panel
    }

    private fun initConfigFileStep(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
        }
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step2Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbTextArea("createConfigFile".text(), TextStyle.BODY))

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.configFieldsLayout)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnConfigFile".text()) {
            val layout = cardLayout.layout as CardLayout
            layout.next(cardLayout)

//            if (dataProvider.validateConfigFields(componentManager.getAppId(), componentManager.getToken())) {
//                dataProvider.createEmbraceFile(componentManager.getAppId(), componentManager.getToken(), this)
//            } else {
//                componentManager.changeResultText(
//                    componentManager.configFileStatusPanel,
//                    "noIdOrTokenError".text(),
//                    false
//                )
//            }
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.configFileStatusPanel)
        return panel
    }

    private fun initDependenciesStep(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
        }

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step3Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbTextArea("addSwazzler".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerExampleCode()))

        panel.add(EmbTextArea("applySwazzlerPlugin".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerPluginExampleCode()))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnModifyGradleFiles".text()) {
            val layout = cardLayout.layout as CardLayout
            layout.next(cardLayout)

//            dataProvider.applicationModules?.let {
//                if (it.isNotEmpty()) {
//                    showGradlePopupIfNecessary(it)
//                } else {
//                    Messages.showErrorDialog("noApplicationModule".text(), "GenericErrorTitle".text())
//                }
//            } ?: Messages.showErrorDialog("noApplicationModule".text(), "GenericErrorTitle".text())
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.gradleResultPanel)
        panel.add(EmbTextArea("applyDependencyDescription".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSdkExampleCode()))
        return panel
    }

    private fun initStartEmbraceStep(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
        }
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbTextArea("step4Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbTextArea("step4Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getStartExampleCode()))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnAddEmbraceStart".text()) {
            val layout = cardLayout.layout as CardLayout
            layout.next(cardLayout)
//            dataProvider.addEmbraceStartMethod(this)
        })
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(componentManager.startResultPanel)
        return panel
    }

    private fun initEmbraceVerificationStep(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
        }
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("step5Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbTextArea("step5Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnVerifyIntegration".text()) {
            val layout = cardLayout.layout as CardLayout
            layout.next(cardLayout)

//            componentManager.showLoadingPopup(it)
//            dataProvider.verifyIntegration(this)
        })

        panel.add(Box.createVerticalStrut(5))
        btnOpenDashboard.isVisible = false
        panel.add(btnOpenDashboard)

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbTextArea("contactInfo".text(), TextStyle.BODY))
        return panel
    }

    override fun onOnboardConnected(appId: String, token: String) {
        componentManager.setAppIdAndToken(appId, token)

        componentManager.changeResultText(
            componentManager.connectEmbraceResultPanel,
            "connectedToEmbraceSuccessfully".text()
        )
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
    }

    override fun onStartStatusUpdated(status: StartMethodStatus) {
        when (status) {
            StartMethodStatus.ERROR -> {
                Messages.showErrorDialog("startMethodError".text(), "GenericErrorTitle".text())
            }

            StartMethodStatus.START_ADDED_SUCCESSFULLY -> {
                componentManager.changeResultText(
                    componentManager.startResultPanel,
                    "startAddedSuccessfully".text()
                )
            }

            StartMethodStatus.START_ALREADY_ADDED -> {
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
