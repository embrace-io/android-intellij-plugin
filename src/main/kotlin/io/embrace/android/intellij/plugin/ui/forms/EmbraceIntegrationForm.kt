package io.embrace.android.intellij.plugin.ui.forms

import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbEditableText
import io.embrace.android.intellij.plugin.ui.components.EmbLabel
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Color
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.JScrollPane


internal class EmbraceIntegrationForm(private val dataProvider: EmbraceIntegrationDataProvider) :
    ConfigFileCreationCallback {
    private val panel = JPanel()
    private val scrollPane = JScrollPane(panel)
    private val errorColor = Color.decode("#d42320")
    private val successColor = Color.decode("#16c74e")
    private val configFileErrorLabel = EmbLabel("", TextStyle.BODY, errorColor)
    private val etAppId = EmbEditableText("sawWz")
    private val etToken = EmbEditableText("123k1jn123998asd")

    init {
        initMainPanel()

        initGetStartedLayout()
        initCreateAppStep()
        initConfigFileStep()
        initBuildConfigFileStep()
        initStartEmbraceStep()
    }

    fun getContent(): JPanel {
        return panel
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
            dataProvider.openDashboard()
        })
    }

    private fun initConfigFileStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step2Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("modifyGradleFile".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbLabel("appIdLabel".text(), TextStyle.HEADLINE_3))
        panel.add(Box.createVerticalStrut(5))
        panel.add(etAppId)
        panel.add(Box.createVerticalStrut(5))
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
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step3Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("addSwazzler".text(), TextStyle.BODY))
        panel.add(EmbLabel("addSwazzlerLine2".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbLabel("addSdk".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getSdkExampleCode()))

        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerExampleCode()))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))

        panel.add(EmbButton("btnModifyGradleFiles".text()) {
            dataProvider.modifyGradleFile()
        })
    }


    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbLabel("step4Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step4Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(VERTICAL_SPACE))
        panel.add(EmbBlockCode(panel, dataProvider.getStartExampleCode()))
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
}

private const val VERTICAL_SPACE = 20
private const val BORDER_TOP = 0
private const val BORDER_BOTTOM = 20
private const val BORDER_LEFT = 20
private const val BORDER_RIGHT = 20
