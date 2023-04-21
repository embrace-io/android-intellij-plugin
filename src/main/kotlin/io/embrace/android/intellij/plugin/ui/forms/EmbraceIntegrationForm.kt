package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBScrollPane
import io.embrace.android.intellij.plugin.EmbraceStringResources
import io.embrace.android.intellij.plugin.constants.CodeType
import io.embrace.android.intellij.plugin.extensions.text
import io.embrace.android.intellij.plugin.network.HttpService
import io.embrace.android.intellij.plugin.ui.components.EmbBlockCode
import io.embrace.android.intellij.plugin.ui.components.EmbButton
import io.embrace.android.intellij.plugin.ui.components.EmbLabel
import io.embrace.android.intellij.plugin.ui.components.TextStyle
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project
import java.awt.Desktop
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel


class EmbraceIntegrationForm {
    private val panel = JPanel()
    private val scrollPane = JBScrollPane()
    private val verticalSpace = 20
    private val httpService = HttpService()


    fun getContent(): JBScrollPane {
        return scrollPane
    }

    init {
        initMainPanel()

        initGetStartedLayout()
        initCreateAppStep()
        initConfigFileStep()
        initBuildConfigFileStep()
        initStartEmbraceStep()
        scrollPane.setViewportView(panel)
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
            createEmbraceFile(project?.basePath)
        })
    }

    private fun initBuildConfigFileStep() {
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbLabel("step3Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step3Description".text(), TextStyle.BODY))

        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbBlockCode(panel, CodeType.SDK, httpService))

        panel.add(EmbLabel("addSwazzler".text(), TextStyle.BODY))
        panel.add(EmbLabel("addSwazzlerLine2".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(verticalSpace))

        panel.add(EmbBlockCode(panel, CodeType.SWAZZLER, httpService))
        panel.add(Box.createVerticalStrut(verticalSpace))

        panel.add(EmbButton("btnModifyGradleFiles".text()) {
            modifyGradleFile(project?.basePath)
        })
    }


    private fun initStartEmbraceStep() {
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbLabel("step4Title".text(), TextStyle.HEADLINE_2))
        panel.add(EmbLabel("step4Description".text(), TextStyle.BODY))
        panel.add(Box.createVerticalStrut(verticalSpace))
        panel.add(EmbBlockCode(panel, CodeType.START_EMBRACE, httpService))
    }


    // All the following things should be moved to a data layer
    // ----------------------------------------------------------

    private
    val FILE_ROOT = "file://"

    private
    val MAIN_PATH = "/app/src/main"

    private
    val EMBRACE_CONFIG_FILE = "/embrace-config.json"


    private fun modifyGradleFile(basePath: String?) {
        try {
            val file = File("$basePath/build.gradle")
            val sb =
                """// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:7.0.0"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10"
        classpath "io.embrace:embrace-swazzler:5.14.0"

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}"""
            val writer = PrintWriter(file)
            writer.write(sb)
            writer.close()
        } catch (e: IOException) {
            println("An error occurred reading build.gradle file.")
            e.printStackTrace()
        }
    }

    private fun createEmbraceFile(basePath: String?) {
        try {
            val file = File(basePath + MAIN_PATH + EMBRACE_CONFIG_FILE)
            val writer: FileWriter = FileWriter(file)
            writer.write(
                """{
  "app_id": "hU4P8",
  "api_token": "13f327e891ad45858949004eb755b9f1",
  "ndk_enabled": false
}"""
            )
            writer.close()

            // Refresh the folder containing the new file
            val parentFolder = VirtualFileManager.getInstance()
                .findFileByUrl(FILE_ROOT + basePath + MAIN_PATH)
            if (parentFolder != null) {
                ApplicationManager.getApplication().runWriteAction {
                    parentFolder.refresh(
                        false,
                        false
                    )
                }
            }
            println("File created: " + file.name)
        } catch (e: Exception) {
            println("An error occurred on embrace-config file creation.")
            e.printStackTrace()
        }
    }


}