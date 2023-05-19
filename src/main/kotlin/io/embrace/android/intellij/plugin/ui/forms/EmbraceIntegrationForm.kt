package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.SwazzlerPluginAddedCallback
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbEditableText
import io.embrace.android.intellij.plugin.ui.components.EmbLabel
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Color
import java.awt.Rectangle
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JOptionPane
import javax.swing.JPanel


private const val VERTICAL_SPACE = 20
private const val BORDER_TOP = 0
private const val BORDER_BOTTOM = 20
private const val BORDER_LEFT = 20
private const val BORDER_RIGHT = 30


internal class EmbraceIntegrationForm(
    private val dataProvider: EmbraceIntegrationDataProvider
) : ConfigFileCreationCallback,
    ProjectGradleFileModificationCallback,
    SwazzlerPluginAddedCallback {

    internal val panel = JPanel()
    private val scrollPane = JBScrollPane()
    private val errorColor = Color.decode("#d42320")
    private val successColor = Color.decode("#16c74e")
    private val configFileErrorLabel = EmbLabel("", TextStyle.BODY, errorColor)
    private val etAppId = EmbEditableText("Eg: sawWz")
    private val etToken = EmbEditableText("Eg: 123k1jn123998asd")
    private val btnGradleFiles = EmbButton("btnModifyGradleFiles".text()) {
        dataProvider.getGradleContentToModify(this)
    }

    init {
        initMainPanel()

        initGetStartedLayout()
        initCreateAppStep()
        initConfigFileStep()
        initBuildConfigFileStep()
        initStartEmbraceStep()

        scrollPane.viewport.view = panel
        scrollPane.scrollRectToVisible(Rectangle(0, 0, 1, 1))
        scrollPane.verticalScrollBar.value = 0

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
            dataProvider.startServer(etAppId, etToken)
            dataProvider.openDashboard()
        })
    }

    private fun initConfigFileStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step2Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("modifyGradleFile".text(), TextStyle.BODY))

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
        panel.add(btnGradleFiles)

        panel.add(EmbLabel("applyDependencyDescription".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSdkExampleCode()))
    }


    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step4Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step4Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getStartExampleCode()))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbButton("btnAddEmbraceStart".text()) {
            dataProvider.addEmbraceStartMethod()
        })
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
        btnGradleFiles.isEnabled = true
        Messages.showInfoMessage(
            error,
            "Error"
        )
    }


    override fun onGradleContentFound(newLine: String, contentToModify: String) {
        val options = arrayOf<Any>("Replace", "Cancel")

        val message = contentToModify.replace(newLine, "*** $newLine *** ")
        val completeMessage = "Confirm the following changes to your build.gradle file:\n\n $message"


        val choice = JOptionPane.showOptionDialog(
            null,
            completeMessage,
            "Add Swazzler Plugin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )

        if (choice == JOptionPane.YES_OPTION) {
            dataProvider.modifyGradleFile(contentToModify, this)
        } else if (choice == JOptionPane.NO_OPTION) {
            showAddSwazzlerPluginDialog(false)
        }
    }

    override fun onGradleContentModified() {
        showAddSwazzlerPluginDialog(true)
    }

    private fun showAddSwazzlerPluginDialog(projectFileWasModified: Boolean) {
        val options = arrayOf<Any>("Replace", "Cancel")

        val message = if (projectFileWasModified) {
            "swazzlerFileSuccessfullyModified".text() + "\n"
            "addSwazzlerPlugin".text()
        } else {
            "addSwazzlerPlugin".text()
        }

        val choice = JOptionPane.showOptionDialog(
            null,
            message,
            "Add Swazzler Plugin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )

        if (choice == JOptionPane.YES_OPTION) {
            dataProvider.addSwazzlerPlugin(this)
        } else
            btnGradleFiles.isEnabled = true
    }

    override fun onSwazzlerPluginAdded() {
        btnGradleFiles.isEnabled = true
        Messages.showInfoMessage(
            "SwazzlerPluginAdded".text(),
            "Info"
        )
    }

    override fun onSwazzlerPluginError(error: String) {
        btnGradleFiles.isEnabled = true
        Messages.showInfoMessage(
            error,
            "Error"
        )
    }

}
