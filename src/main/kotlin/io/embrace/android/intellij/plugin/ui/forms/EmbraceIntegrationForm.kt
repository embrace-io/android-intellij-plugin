package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.ui.components.JBScrollPane
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.utils.extensions.text
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbLabel
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project
import java.awt.Desktop
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

class EmbraceIntegrationForm {
    private val dataProvider = EmbraceIntegrationDataProvider()
    private val panel = JPanel()
    private val scrollPane = JBScrollPane()
    private val verticalSpace = 20

    init {
        initMainPanel()

        initGetStartedLayout()
        initCreateAppStep()
        initConfigFileStep()
        initBuildConfigFileStep()
        initStartEmbraceStep()
        scrollPane.setViewportView(panel)
    }

    fun getContent(): JBScrollPane {
        return scrollPane
    }

    private fun initMainPanel() {
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(0, 20, 20, 20)
        //        val frame = JFrame("popup");
    }

    private fun initGetStartedLayout() {
        panel.add(EmbLabel("getStartedTitle".text(), TextStyle.HEADLINE_1))
        panel.add(EmbLabel("getStartedDescription".text(), TextStyle.BODY))
    }

    private fun initCreateAppStep() {
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbLabel("step1Title".text(), TextStyle.HEADLINE_2))
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbButton("btnConnect".text()) {
            val url = "https://dash.embrace.io/onboard/project"
            try {
                Desktop.getDesktop().browse(URI(url))
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        })
    }

    private fun initConfigFileStep() {
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbLabel("step2Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("modifyGradleFile".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbButton("btnConfigFile".text()) {
            dataProvider.createEmbraceFile(project?.basePath, "appId", "token")
        })
    }

    private fun initBuildConfigFileStep() {
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbLabel("step3Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step3Description".text(), TextStyle.BODY))

        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbBlockCode(panel, dataProvider.getSdkExampleCode()))

        panel.add(EmbLabel("addSwazzler".text(), TextStyle.BODY))
        panel.add(EmbLabel("addSwazzlerLine2".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(verticalSpace))

        panel.add(EmbBlockCode(panel, dataProvider.getSwazzlerExampleCode()))
        panel.add(Box.createVerticalStrut(verticalSpace))

        panel.add(EmbButton("btnModifyGradleFiles".text()) {
            dataProvider.modifyGradleFile(project?.basePath)
        })
    }


    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbLabel("step4Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step4Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbBlockCode(panel, dataProvider.getStartExampleCode()))
    }
}
