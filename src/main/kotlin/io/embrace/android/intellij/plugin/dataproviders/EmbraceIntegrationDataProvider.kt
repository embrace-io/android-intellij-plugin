package io.embrace.android.intellij.plugin.dataproviders

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpServer
import io.embrace.android.intellij.plugin.data.AppModule
import io.embrace.android.intellij.plugin.data.BuildType
import io.embrace.android.intellij.plugin.data.EmbraceProject
import io.embrace.android.intellij.plugin.data.GradleFileStatus
import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.OnboardConnectionCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.VerifyIntegrationCallback
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.embrace.android.intellij.plugin.repository.gradle.BuildGradleFilesModifier
import io.embrace.android.intellij.plugin.repository.network.ApiService
import io.embrace.android.intellij.plugin.repository.network.OnboardConnectionCallbackHandler
import io.embrace.android.intellij.plugin.utils.extensions.text
import io.sentry.Sentry
import java.awt.Desktop
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException

private const val VERIFICATION_COUNT_MAX = 5
private const val RETRY_TIME = 2000L

internal class EmbraceIntegrationDataProvider(
    private val project: Project,
    private val repo: EmbracePluginRepository = EmbracePluginRepository(project)
) {
    internal val CONTACT_EMAIL: String = "support@embrace.io"
    private var embraceProject: EmbraceProject? = null
    private var callbackPort: Int = 0
    private val lastEmbraceVersion = repo.getLastSDKVersion()
    internal var applicationModules: List<AppModule>? = null
    private var verificationCounter = 0

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

    fun connectToEmbrace(callback: OnboardConnectionCallback) {
        startServer(callback)

        val url = buildOnboardDashURL()
        Desktop.getDesktop().browse(URI(url))
    }

    private fun startServer(callback: OnboardConnectionCallback) {
        val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        val handler = OnboardConnectionCallbackHandler({
            embraceProject = it
            callback.onOnboardConnected(it.appId, it.token)
        }, {
            callback.onOnboardConnectedError(it)
        })

        server.createContext("/", handler)
        server.executor = null
        server.start()
        callbackPort = server.address.port
        println("Server started on port $callbackPort")
    }

    internal fun createConfigurationEmbraceFile(
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

            if (repo.createEmbraceConfigFile(configFile, path)) {
                callback.onConfigSuccess()
            } else {
                callback.onConfigError("configFileCreationError".text())
            }

        } ?: callback.onConfigError("configFilePathNotFoundError".text())
    }

    fun validateConfigFields(appId: String, token: String) =
        appId.length == APP_ID_LENGTH && token.length == TOKEN_LENGTH


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


    fun modifyGradleFile(selectedModule: AppModule, callback: ProjectGradleFileModificationCallback) {
        val rootFileStatus = buildGradleFilesModifier.value?.addSwazzlerClasspath()
        val appFileStatus = buildGradleFilesModifier.value?.addSwazzlerPlugin(selectedModule)

        if (rootFileStatus == GradleFileStatus.ADDED_SUCCESSFULLY
            && appFileStatus == GradleFileStatus.ADDED_SUCCESSFULLY
        ) {
            buildGradleFilesModifier.value?.syncGradle(project)
            callback.onGradleFilesModifiedSuccessfully()
        } else if (rootFileStatus == GradleFileStatus.SWAZZLER_ALREADY_ADDED) {
            callback.onGradleFileAlreadyModified()
        } else if (rootFileStatus == GradleFileStatus.DEPENDENCIES_BLOCK_NOT_FOUND) {
            callback.onGradleFileError("dependenciesBlockNotFoundError".text())
        } else if (rootFileStatus == GradleFileStatus.FILE_NOT_FOUND
            || appFileStatus == GradleFileStatus.FILE_NOT_FOUND
        ) {
            callback.onGradleFileError("gradleFileNotFound".text())
        } else
            callback.onGradleFileError("oneOrMoreFilesError".text())

    }

    fun addEmbraceStartMethod(callback: StartMethodCallback) {
        if (buildGradleFilesModifier.value?.appPackageName == null) {
            buildGradleFilesModifier.value?.retrievePackageName()
        }
        repo.addStartToApplicationClass(buildGradleFilesModifier.value?.appPackageName, callback)
    }

    fun verifyIntegration(callback: VerifyIntegrationCallback) {
        embraceProject?.also {
            if (it.sessionId != null) {
                repo.verifyIntegration(it, {
                    verificationCounter = 0
                    callback.onEmbraceIntegrationSuccess()
                }, {
                    if (verificationCounter >= VERIFICATION_COUNT_MAX) {
                        verificationCounter = 0
                        callback.onEmbraceIntegrationError()
                    } else {
                        Thread.sleep(RETRY_TIME)
                        verifyIntegration(callback)
                    }
                })
                verificationCounter++
            } else {
                callback.onEmbraceIntegrationError()
            }
        } ?: callback.onEmbraceIntegrationError()
    }

    private fun getResourceAsText(path: String): String? =
        object {}.javaClass.getResource(path)?.readText()

    private fun buildOnboardDashURL(): String {
        val projectName = repo.getProjectName().replace(" ", "")
        return ApiService.EMBRACE_CREATE_PROJECT_URL + "?project_name=$projectName&localhost_port=$callbackPort"
    }

    fun openDashboard() {

        embraceProject?.let {
            try {
                val url = ApiService.EMBRACE_DASHBOARD_URL.replace("{appId}", it.appId)
                Desktop.getDesktop().browse(URI(url))
            } catch (ex: IOException) {
                Sentry.captureException(ex)
            } catch (ex: URISyntaxException) {
                Sentry.captureException(ex)
            }
        }
    }

    fun getSwazzlerClasspathLine(): String {
        return buildGradleFilesModifier.value?.getClasspathSwazzlerLine(false) ?: ""
    }

    fun getSwazzlerPluginLine(buildType: BuildType): String {
        return buildGradleFilesModifier.value?.getPluginSwazzlerLine(buildType) ?: ""
    }

    fun sendSupportEmail() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            val uri = URI("mailto:$CONTACT_EMAIL")
            Desktop.getDesktop().mail(uri)
        }
    }
}
