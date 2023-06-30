package io.embrace.android.intellij.plugin.dataproviders

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpServer
import io.embrace.android.intellij.plugin.data.AppModule
import io.embrace.android.intellij.plugin.data.GradleFileStatus
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.OnboardConnectionCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.embrace.android.intellij.plugin.repository.gradle.BuildGradleFilesModifier
import io.embrace.android.intellij.plugin.repository.network.OnboardConnectionCallbackHandler
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI


internal class EmbraceIntegrationDataProvider(
    private val project: Project,
    private val repo: EmbracePluginRepository = EmbracePluginRepository(project)
) {

    private lateinit var appId: String
    private var callbackPort: Int = 0
    private val lastEmbraceVersion = repo.getLastSDKVersion()
    internal var applicationModules: List<AppModule>? = null


    private val buildGradleFilesModifier = lazy {
        project.basePath?.let {
            BuildGradleFilesModifier(project, lastEmbraceVersion)
        }
    }

    companion object {
        private const val APP_ID_LENGTH = 5
        private const val TOKEN_LENGTH = 32
    }

    init {
        loadApplicationModules()
    }

    private fun loadApplicationModules() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val modules = buildGradleFilesModifier.value?.getModules()
            ApplicationManager.getApplication().invokeLater {
                applicationModules = buildGradleFilesModifier.value?.getApplicationModules(modules)
            }
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

    fun connectToEmbrace(callback: OnboardConnectionCallback) {
        startServer(callback)
        openDashboard()
    }

    fun getSwazzlerClasspathLine() =
        EmbracePluginRepository.EMBRACE_SWAZZLER_CLASSPATH.replace("LAST_VERSION", lastEmbraceVersion)

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


    fun modifyGradleFile(selectedModule: String, callback: ProjectGradleFileModificationCallback) {
        val rootFileStatus = buildGradleFilesModifier.value?.updateBuildGradleFileContent()
        val appFileStatus = buildGradleFilesModifier.value?.addSwazzlerPlugin(selectedModule)

        if (rootFileStatus == GradleFileStatus.ADDED_SUCCESSFULLY
            && appFileStatus == GradleFileStatus.ADDED_SUCCESSFULLY
        ) {
            callback.onGradleFilesModifiedSuccessfully()
        } else if (rootFileStatus == GradleFileStatus.SWAZZLER_ALREADY_ADDED) {
            callback.onGradleFileAlreadyModified()
        } else if (rootFileStatus == GradleFileStatus.FILE_NOT_FOUND
            || appFileStatus == GradleFileStatus.FILE_NOT_FOUND
        ) {
            callback.onGradleFileError("gradleFileNotFound".text())
        } else
            callback.onGradleFileError("oneOrMoreFilesError".text())

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

            if (repo.createEmbraceConfigFile(configFile, path)) {
                callback.onConfigSuccess()
            } else {
                callback.onConfigError("cannot create config file")
            }

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
        val projectName = repo.getProjectName().replace(" ","")
        return EmbracePluginRepository.embraceDashboardUrl + "?project_name=$projectName&localhost_port=$callbackPort"
    }


}
