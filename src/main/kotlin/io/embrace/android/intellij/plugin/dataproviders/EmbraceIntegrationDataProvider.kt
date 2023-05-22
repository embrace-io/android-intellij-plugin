package io.embrace.android.intellij.plugin.dataproviders

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.sun.net.httpserver.HttpServer
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.OnboardConnectionCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.SwazzlerPluginAddedCallback
import io.embrace.android.intellij.plugin.gradle.BuildGradleFilesModifier
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.embrace.android.intellij.plugin.repository.network.OnboardConnectionCallbackHandler
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Desktop
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI


internal class EmbraceIntegrationDataProvider(
    private val repo: EmbracePluginRepository,
    private val project: Project
) {
    private var callbackPort: Int = 0
    private val lastEmbraceVersion = repo.getLastSDKVersion()
    private val buildGradleFilesModifier = lazy {
        project.basePath?.let {
            BuildGradleFilesModifier(project, lastEmbraceVersion)
        }
    }

    fun connectToEmbrace(callback: OnboardConnectionCallback) {
        startServer(callback)
        openDashboard()
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

    fun addEmbraceStartMethod() {
        val applicationClass = repo.getApplicationClass(project)

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

    fun validateConfigFields(appId: String, token: String) =
        appId.length == APP_ID_LENGTH && token.length == TOKEN_LENGTH

    private fun startServer(callback: OnboardConnectionCallback) {
        val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/", OnboardConnectionCallbackHandler(callback))
        server.executor = null
        server.start()
        callbackPort = server.address.port
        println("Server started on port $callbackPort")
    }

    private fun openDashboard() {
        try {
            val url = buildOnboardDashURL()
            Desktop.getDesktop().browse(URI(url))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun getResourceAsText(path: String): String? =
        object {}.javaClass.getResource(path)?.readText()

    private fun buildOnboardDashURL(): String {
        val projectName = repo.getProjectName()
        return repo.embraceDashboardUrl + "?projectName=$projectName&platform=Android&localPort=$callbackPort"
    }

    companion object {
        private const val APP_ID_LENGTH = 5
        private const val TOKEN_LENGTH = 32
    }

}
