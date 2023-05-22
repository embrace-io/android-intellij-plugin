package io.embrace.android.intellij.plugin.dataproviders

import com.intellij.openapi.project.Project
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.SwazzlerPluginAddedCallback
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.embrace.android.intellij.plugin.repository.gradle.BuildGradleFilesModifier
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Desktop
import java.io.IOException
import java.net.URI


internal class EmbraceIntegrationDataProvider(
    private val project: Project,
    private val repo: EmbracePluginRepository = EmbracePluginRepository(project)
) {

    private val lastEmbraceVersion = repo.getLastSDKVersion()

    private lateinit var appId: String

    private val buildGradleFilesModifier = lazy {
        project.basePath?.let {
            BuildGradleFilesModifier(project, lastEmbraceVersion)
        }
    }

    companion object {
        private const val APP_ID_LENGTH = 5
        private const val TOKEN_LENGTH = 32
    }

    fun openDashboard() {
        try {
            Desktop.getDesktop().browse(URI(EmbracePluginRepository.embraceDashboardUrl))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun openFinishIntegrationDashboard() {
        try {

            val url = if (::appId.isInitialized) {
                EmbracePluginRepository.embraceDashboardIntegrationUrl.replace("{appId}", appId)
            } else {
                EmbracePluginRepository.embraceDashboardUrl
            }

            Desktop.getDesktop().browse(URI(url))
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

    fun getSwazzlerPluginExampleCode(): String {
        val code = getResourceAsText("/examplecode/plugin.txt") ?: ""
        return code.replace("LAST_VERSION", lastEmbraceVersion)
    }


    fun getStartExampleCode(): String {
        return getResourceAsText("/examplecode/embrace_start.txt") ?: ""
    }

    fun getGradleContentToModify(callback: ProjectGradleFileModificationCallback) {
        buildGradleFilesModifier.value?.getBuildGradleFileContent(callback)
    }

    fun modifyGradleFile(content: String, callback: ProjectGradleFileModificationCallback) {
        try {
            buildGradleFilesModifier.value?.updateBuildGradleFileForProject(content)
            callback.onGradleContentModified()
        } catch (e: IOException) {
            callback.onGradleFileError("cannotModifyGradle".text())
        }
    }

    fun addSwazzlerPlugin(callback: SwazzlerPluginAddedCallback) {
        buildGradleFilesModifier.value?.addSwazzlerPlugin(callback)
    }

    fun addEmbraceStartMethod(callback: StartMethodCallback) {
        repo.addStartToApplicationClass(callback)
    }

    internal fun createEmbraceFile(
        appId: String,
        token: String,
        callback: ConfigFileCreationCallback,
        shouldOverrideFile: Boolean? = false
    ) {
        this.appId = appId

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


}
