package io.embrace.android.intellij.plugin.dataproviders

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.gradle.BuildGradleFilesModifier
import io.embrace.android.intellij.plugin.gradle.GradleToolingApiWrapper
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import java.awt.Desktop
import java.io.IOException
import java.net.URI


internal class EmbraceIntegrationDataProvider(
    private val repo: EmbracePluginRepository,
    private val project: Project,
    private val basePath: String?
) {
    private val lastEmbraceVersion = repo.getLastSDKVersion()

    fun openDashboard() {
        try {
            Desktop.getDesktop().browse(URI(repo.embraceDashboardUrl))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getSdkExampleCode(): String {
        val code = getResourceAsText("/examplecode/sdk.txt") ?: ""
        return code.replace("LAST_VERSION", lastEmbraceVersion)
    }

    fun getSwazzlerExampleCode(): String {
        val code = getResourceAsText("/examplecode/swazzler.txt") ?: ""
        return code.replace("LAST_VERSION", lastEmbraceVersion)
    }

    fun getStartExampleCode(): String {
        return getResourceAsText("/examplecode/embrace_start.txt") ?: ""
    }

    fun modifyGradleFile() {
        try {
            project.basePath?.let { path ->
                val gradleToolingApiWrapper = GradleToolingApiWrapper(path)
                val buildGradleFilesModifier =
                    BuildGradleFilesModifier(project, gradleToolingApiWrapper, lastEmbraceVersion)
                buildGradleFilesModifier.updateAllBuildGradleFiles()
            }
        } catch (e: IOException) {
            println("An error occurred reading build.gradle file.")
            e.printStackTrace()
        }
    }

    fun addEmbraceStartMethod() {

        val applicationClass = repo.getApplicationClass(project, basePath)

        applicationClass?.let {
            val result = Messages.showYesNoDialog(
                project,
                "Application Class detected. Would you like to proceed to add Embrace.Start sentence?",
                "Confirmation",
                Messages.getQuestionIcon()
            )

            if (result == Messages.YES) {
                repo.addEmbraceStartToApplicationClass(applicationClass, project)
            }
        } ?: Messages.showInfoMessage(
            "There is no application class in your project, please add Embrace.Start manually",
            "Info"
        )
    }

    internal fun createEmbraceFile(
        appId: String,
        token: String,
        callback: ConfigFileCreationCallback,
        shouldOverrideFile: Boolean? = false
    ) {
        project.basePath?.let { path ->
            val isFileAlreadyCreated = repo.isConfigurationAlreadyCreated(path)

            if (isFileAlreadyCreated && shouldOverrideFile == false) {
                callback.onConfigAlreadyExists()
                return
            }

            var configFile = getResourceAsText("/examplecode/config_template.txt") ?: ""
            configFile = configFile.replace("MY_APP_ID", appId)
            configFile = configFile.replace("MY_TOKEN", token)

            if (repo.createEmbraceConfigFile(configFile, path))
                callback.onConfigSuccess()
            else
                callback.onConfigError("cannot create config file")

        } ?: callback.onConfigError("cannot get the path")
    }

    private fun getResourceAsText(path: String): String? =
        object {}.javaClass.getResource(path)?.readText()

    fun validateConfigFields(appId: String, token: String) =
        appId.length == APP_ID_LENGTH && token.length == TOKEN_LENGTH

    companion object {
        private const val APP_ID_LENGTH = 5
        private const val TOKEN_LENGTH = 32
    }

}
