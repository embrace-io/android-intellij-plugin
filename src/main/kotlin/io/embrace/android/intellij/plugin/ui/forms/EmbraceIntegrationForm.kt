package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBScrollPane
import io.embrace.android.intellij.plugin.data.AppModule
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.data.StartMethodStatus
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.OnboardConnectionCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbEditableText
import io.embrace.android.intellij.plugin.ui.components.EmbLabel
import io.embrace.android.intellij.plugin.ui.components.FormComponentManager
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.utils.extensions.text
import org.intellij.datavis.r.inlays.table.filters.gui.FilterSettings.errorColor
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project
import java.awt.Color
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
    OnboardConnectionCallback {

    internal val panel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
    }

    private val scrollPane = JBScrollPane(panel)
    private val componentManager = FormComponentManager()
    private val etAppId = EmbEditableText("Eg: sawWz")
    private val etToken = EmbEditableText("Eg: 123k1jn123998asd")
    private var gradlePopup: GradleFilesPopup? = null

    init {
        SwingUtilities.invokeLater {
            initGetStartedLayout()
            initCreateAppStep()
            initConfigFileStep()
            initDependenciesStep()
            initStartEmbraceStep()
            initEmbraceVerificationStep()
            scrollToTop()
        }
    }

    private fun scrollToTop() {
        // Add HierarchyListener to detect when the view is added to the scroll pane, scroll top and remove it.
        scrollPane.addHierarchyListener(object : HierarchyListener {
            override fun hierarchyChanged(e: HierarchyEvent) {
                if ((e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && scrollPane.isShowing) {
                    // Scroll to the top
                    scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.minimum
                    scrollPane.horizontalScrollBar.value = scrollPane.horizontalScrollBar.minimum
                    // Remove the HierarchyListener
                    scrollPane.removeHierarchyListener(this)
                }
            }
        })
    }

    fun getContent(): JBScrollPane {
        return scrollPane
    }

    private fun initGetStartedLayout() {
        panel.add(EmbLabel("getStartedTitle".text(), TextStyle.HEADLINE_1))
        panel.add(EmbLabel("getStartedDescription".text(), TextStyle.BODY))
    }

    private fun initCreateAppStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step1Title".text(), TextStyle.HEADLINE_2))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE_SMALL))
        panel.add(EmbLabel("step1Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnConnect".text()) {
            dataProvider.connectToEmbrace(this)
        })


        panel.add(componentManager.connectToEmbraceResultLabel)
    }

    private fun initConfigFileStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step2Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("createConfigFile".text(), TextStyle.BODY))

        panel.add(EmbLabel("appIdLabel".text(), TextStyle.HEADLINE_3))
        panel.add(Box.createVerticalStrut(5))
        panel.add(etAppId)

        panel.add(EmbLabel("tokenLabel".text(), TextStyle.HEADLINE_3))
        panel.add(Box.createVerticalStrut(5))
        panel.add(etToken)
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        componentManager.configFileStatusLabel.isVisible = false

        panel.add(EmbButton("btnConfigFile".text()) {
            if (dataProvider.validateConfigFields(etAppId.text, etToken.text)) {
                dataProvider.createEmbraceFile(etAppId.text, etToken.text, this)
            } else {
                componentManager.configFileStatusLabel.text = "noIdOrTokenError".text()
                componentManager.configFileStatusLabel.isVisible = true
            }
        })

        panel.add(componentManager.configFileStatusLabel)
    }

    private fun initDependenciesStep() {
        panel.add(EmbLabel("step3Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("addSwazzler".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerExampleCode()))

        panel.add(EmbLabel("applySwazzlerPlugin".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerPluginExampleCode()))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnModifyGradleFiles".text()) {
            dataProvider.applicationModules?.let {
                if (it.isNotEmpty()) {
                    showGradlePopupIfNecessary(it)
                } else {
                    Messages.showErrorDialog("noApplicationModule".text(), "GenericErrorTitle".text())
                }
            } ?: Messages.showErrorDialog("noApplicationModule".text(), "GenericErrorTitle".text())
        })


        panel.add(componentManager.gradleResultPanel)
        panel.add(EmbLabel("applyDependencyDescription".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSdkExampleCode()))
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

    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step4Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step4Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getStartExampleCode()))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnAddEmbraceStart".text()) {
            dataProvider.addEmbraceStartMethod(this)
        })
        componentManager.startResultLabel.isVisible = false
        panel.add(componentManager.startResultLabel)
    }

    private fun initEmbraceVerificationStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step5Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step5Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnVerifyIntegration".text()) {
            dataProvider.openFinishIntegrationDashboard()
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("contactInfo".text(), TextStyle.BODY))
    }

    override fun onConfigSuccess() {
        componentManager.configFileStatusLabel.foreground = componentManager.successColor
        componentManager.configFileStatusLabel.text = "configFileCreated".text()
        componentManager.configFileStatusLabel.isVisible = true
    }

    override fun onConfigAlreadyExists() {
        val options = arrayOf("Replace", "Cancel")
        val result =
            Messages.showDialog("replaceConfig".text(), "Replace Configuration", options, 0, Messages.getQuestionIcon())

        if (result == 0) {
            dataProvider.createEmbraceFile(etAppId.text, etToken.text, this, true)
        }
    }

    override fun onConfigError(error: String) {
        componentManager.configFileStatusLabel.foreground = componentManager.errorColor
        componentManager.configFileStatusLabel.text = error
        componentManager.configFileStatusLabel.isVisible = true
    }

    override fun onGradleFileError(error: String) {
        Messages.showInfoMessage(
            error,
            "Error"
        )
    }

    override fun onGradleFileAlreadyModified() {
        componentManager.gradleResultPanel.isVisible = true
        Messages.showInfoMessage(
            "gradleFilesAlreadyAdded".text(),
            "Info"
        )
    }

    override fun onGradleFilesModifiedSuccessfully() {
        componentManager.gradleResultPanel.isVisible = true
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
                componentManager.startResultLabel.isVisible = true
                "startAddedSuccessfully".text()
            }

            StartMethodStatus.START_ALREADY_ADDED -> {
                componentManager.startResultLabel.isVisible = true
                Messages.showInfoMessage("startAlreadyAdded".text(), "")
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_FOUND -> {
                Messages.showErrorDialog("applicationClassNotFound".text(), "GenericErrorTitle".text())
            }

            StartMethodStatus.APPLICATION_CLASS_NOT_ON_CREATE -> {
                Messages.showErrorDialog("applicationClassNotOnCreate".text(), "GenericErrorTitle".text())
            }

        }
    }


    override fun onOnboardConnected(appId: String, token: String) {
        etAppId.text = appId
        etToken.text = token
        componentManager.connectToEmbraceResultLabel.text = "connectedToEmbraceSuccessfully".text()
        componentManager.connectToEmbraceResultLabel.foreground = componentManager.successColor
        componentManager.connectToEmbraceResultLabel.isVisible = true
    }

    override fun onOnboardConnectedError(error: String) {
        componentManager.connectToEmbraceResultLabel.text = "connectedToEmbraceError".text()
        componentManager.connectToEmbraceResultLabel.foreground = componentManager.errorColor
        componentManager.connectToEmbraceResultLabel.isVisible = true
    }


}
