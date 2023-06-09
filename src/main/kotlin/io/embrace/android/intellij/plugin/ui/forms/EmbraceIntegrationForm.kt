package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.dataproviders.StartMethodStatus
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.OnboardConnectionCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbEditableText
import io.embrace.android.intellij.plugin.ui.components.EmbLabel
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.utils.extensions.text
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator


private const val VERTICAL_SPACE = 20
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

    internal val panel = JPanel()
    private val scrollPane = JBScrollPane(panel)
    private val errorColor = Color.decode("#d42320")
    private val successColor = Color.decode("#16c74e")
    private val connectToEmbraceResultLabel = EmbLabel("", TextStyle.BODY, errorColor)
    private val configFileErrorLabel = EmbLabel("", TextStyle.BODY, errorColor)
    private val etAppId = EmbEditableText("Eg: sawWz")
    private val etToken = EmbEditableText("Eg: 123k1jn123998asd")
    private var gradlePopup: GradleFilesPopup? = null

    init {

        initMainPanel()

        initGetStartedLayout()
        initCreateAppStep()
        initConfigFileStep()
        initBuildConfigFileStep()
        initStartEmbraceStep()
        initEmbraceVerificationStep()


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

    private fun initMainPanel() {
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT)
    }

    private fun initGetStartedLayout() {
        panel.add(EmbLabel("getStartedTitle".text(), TextStyle.HEADLINE_1))
        panel.add(EmbLabel("getStartedDescription".text(), TextStyle.BODY))
    }

    private fun initCreateAppStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step1Title".text(), TextStyle.HEADLINE_2))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step1Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnConnect".text()) {
            dataProvider.connectToEmbrace(this)
        })
        connectToEmbraceResultLabel.isVisible = false
        panel.add(connectToEmbraceResultLabel)
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

        panel.add(EmbButton("btnConfigFile".text()) {
            if (dataProvider.validateConfigFields(etAppId.text, etToken.text)) {
                configFileErrorLabel.isVisible = false
                dataProvider.createEmbraceFile(etAppId.text, etToken.text, this)
            } else {
                configFileErrorLabel.text = "noIdOrTokenError".text()
                configFileErrorLabel.isVisible = true
            }
        })

        panel.add(configFileErrorLabel)
    }

    private fun initBuildConfigFileStep() {
        panel.add(EmbLabel("step3Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("addSwazzler".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerExampleCode()))

        panel.add(EmbLabel("applySwazzlerPlugin".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerPluginExampleCode()))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnModifyGradleFiles".text()) {
            val applicationModules = dataProvider.getApplicationModules()
            if (!applicationModules.isNullOrEmpty()) {
                showGradlePopupIfNecessary(applicationModules)
            } else {
                Messages.showErrorDialog("NoApplicationModule".text(), "Error")
            }
        })

        panel.add(EmbLabel("applyDependencyDescription".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSdkExampleCode()))
    }

    private fun showGradlePopupIfNecessary(applicationModules: List<String>) {
        if (gradlePopup == null) {
            gradlePopup = GradleFilesPopup(
                dataProvider,
                applicationModules
            ) { dataProvider.modifyGradleFile(it, this@EmbraceIntegrationForm) }
        }

        if (!gradlePopup!!.isVisible) {
            gradlePopup?.showPopup()
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
    }

    private fun initEmbraceVerificationStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step5Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step5Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnOpenDashboard".text()) {
            dataProvider.openFinishIntegrationDashboard()
        })

        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("contactInfo".text(), TextStyle.BODY))
    }

    override fun onConfigSuccess() {
        configFileErrorLabel.foreground = successColor
        configFileErrorLabel.text = "configFileCreated".text()
        configFileErrorLabel.isVisible = true
    }

    override fun onConfigAlreadyExists() {
        val options = arrayOf<Any>("Replace", "Cancel")

        val choice = JOptionPane.showOptionDialog(
            null,
            "replaceConfig".text(),
            "Replace Configuration",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )

        if (choice == JOptionPane.YES_OPTION) {
            dataProvider.createEmbraceFile(etAppId.text, etToken.text, this, true)
        }
    }

    override fun onConfigError(error: String) {
        configFileErrorLabel.text = error
        configFileErrorLabel.isVisible = true
    }

    override fun onGradleFileError(error: String) {
        Messages.showInfoMessage(
            error,
            "Error"
        )
    }

    override fun onGradleFileAlreadyModified() {
        Messages.showInfoMessage(
            "gradleFilesAlreadyAdded".text(),
            "Info"
        )
    }

    override fun onGradleFilesModifiedSuccessfully() {
        Messages.showInfoMessage(
            "SwazzlerPluginAdded".text(),
            "Info"
        )
    }

    override fun onStartStatusUpdated(status: StartMethodStatus) {
        val message = when (status) {
            StartMethodStatus.ERROR -> "StartMethodError".text()
            StartMethodStatus.START_ADDED_SUCCESSFULLY -> "StartAddedSuccessfully".text()
            StartMethodStatus.START_ALREADY_ADDED -> "StartAlreadyAdded".text()
            StartMethodStatus.APPLICATION_CLASS_NOT_FOUND -> "ApplicationClassNotFound".text()
            StartMethodStatus.APPLICATION_CLASS_NOT_ON_CREATE -> "ApplicationClassNotOnCreate".text()
        }

        Messages.showMessageDialog(project, message, "Embrace", Messages.getInformationIcon())
    }


    override fun onOnboardConnected(appId: String, token: String) {
        etAppId.text = appId
        etToken.text = token
        connectToEmbraceResultLabel.text = "connectedToEmbraceSuccessfully".text()
        connectToEmbraceResultLabel.foreground = successColor
        connectToEmbraceResultLabel.isVisible = true
    }

    override fun onOnboardConnectedError(error: String) {
        connectToEmbraceResultLabel.text = "connectedToEmbraceError".text()
        connectToEmbraceResultLabel.foreground = errorColor
        connectToEmbraceResultLabel.isVisible = true
    }


}
