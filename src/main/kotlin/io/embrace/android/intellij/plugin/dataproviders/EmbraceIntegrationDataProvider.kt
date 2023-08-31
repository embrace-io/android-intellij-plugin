package io.embrace.android.intellij.plugin.dataproviders

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
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

import io.embrace.android.intellij.plugin.repository.sentry.SentryLogger
import io.embrace.android.intellij.plugin.services.TrackingEvent
import io.embrace.android.intellij.plugin.services.TrackingService
import io.embrace.android.intellij.plugin.ui.components.IntegrationStep
import io.embrace.android.intellij.plugin.utils.extensions.text
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI

private const val VERIFICATION_COUNT_MAX = 5
private const val RETRY_TIME = 2500L

internal class EmbraceIntegrationDataProvider(
    private val project: Project,
    private val repo: EmbracePluginRepository = EmbracePluginRepository(project)
) {
    internal val CONTACT_EMAIL: String = "support@embrace.io"
    internal val RESOURCES_LINK: String = "https://embrace.io/docs/android/"
    private var embraceProject: EmbraceProject? = null
    private var callbackPort: Int = 0
    private val lastEmbraceVersion = repo.getLastSDKVersion()
    internal var applicationModules: List<AppModule>? = null
    private var verificationCounter = 0
    private val trackingService = service<TrackingService>()

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

    internal fun loadApplicationModules() {
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
            trackingService.identify(it.externalUserId, it.appId)
            trackingService.trackEvent(TrackingEvent.DASHBOARD_CONNECTED)
            callback.onOnboardConnected(it.appId, it.token)
        }, {
            trackingService.trackEvent(TrackingEvent.DASHBOARD_CONNECTION_FAILED, buildJsonObject {
                put("error", it)
            })
            SentryLogger.logMessage("Error connecting dashboard $it")
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
                SentryLogger.logStepCompleted(IntegrationStep.CONFIG_FILE_CREATION)
                callback.onConfigSuccess()
                trackingService.trackEvent(TrackingEvent.CONFIGURATION_FILE_CREATED)
            } else {
                callback.onConfigError("configFileCreationError".text())
                trackingService.trackEvent(TrackingEvent.CONFIGURATION_FILE_CREATION_FAILED, buildJsonObject {
                    put("error", "configFileCreationError".text())
                })
            }

        } ?: {
            SentryLogger.logMessage("cannot create configuration, project path not found")
            callback.onConfigError("configFilePathNotFoundError".text())
            trackingService.trackEvent(TrackingEvent.CONFIGURATION_FILE_CREATION_FAILED, buildJsonObject {
                put("error", "configFilePathNotFoundError".text())
            })
        }
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

        when {
            rootFileStatus == GradleFileStatus.ADDED_SUCCESSFULLY && appFileStatus == GradleFileStatus.ADDED_SUCCESSFULLY -> {
                buildGradleFilesModifier.value?.syncGradle(project)
                SentryLogger.logStepCompleted(IntegrationStep.DEPENDENCY_UPDATE)
                callback.onGradleFilesModifiedSuccessfully()

                trackingService.trackEvent(TrackingEvent.GRADLE_FILE_MODIFIED)
            }

            rootFileStatus == GradleFileStatus.SWAZZLER_ALREADY_ADDED -> {
                callback.onGradleFileAlreadyModified()

                trackingService.trackEvent(TrackingEvent.GRADLE_FILE_ALREADY_MODIFIED)
            }

            rootFileStatus == GradleFileStatus.FILE_NOT_FOUND && appFileStatus == GradleFileStatus.FILE_NOT_FOUND -> {
                callback.onGradleFileError("oneOrMoreFilesError".text())

                trackingService.trackEvent(TrackingEvent.GRADLE_FILE_MODIFICATION_FAILED, buildJsonObject {
                    put("error", "oneOrMoreFilesError".text())
                })
            }

            else -> {
                callback.onGradleFileError("gradleFileError".text())

                trackingService.trackEvent(TrackingEvent.GRADLE_FILE_MODIFICATION_FAILED, buildJsonObject {
                    put("error", "gradleFileError".text())
                })
            }
        }
    }

    fun addEmbraceStartMethod(callback: StartMethodCallback) {
        if (buildGradleFilesModifier.value?.appPackageName == null) {
            buildGradleFilesModifier.value?.retrievePackageName()
        }
        repo.addStartToApplicationClass(buildGradleFilesModifier.value?.appPackageName, callback)
    }

    fun verifyIntegration(callback: VerifyIntegrationCallback): Boolean {
        if (embraceProject == null || embraceProject!!.sessionId == null) {
            SentryLogger.logMessage("cannot verify integration, embraceProject or session is null")
            callback.onEmbraceIntegrationError()
            return false
        }
        verifyEndpoint(callback)
        return true
    }

    private fun verifyEndpoint(callback: VerifyIntegrationCallback) {
        repo.verifyIntegration(embraceProject!!, {
            verificationCounter = 0
            SentryLogger.logStepCompleted(IntegrationStep.VERIFY_INTEGRATION)
            SentryLogger.endSession()
            callback.onEmbraceIntegrationSuccess()

            trackingService.trackEvent(TrackingEvent.INTEGRATION_SUCCEEDED)
        }, {
            if (verificationCounter >= VERIFICATION_COUNT_MAX) {
                verificationCounter = 0
                callback.onEmbraceIntegrationError()
                SentryLogger.logMessage("cannot verify integration, max retries reached")

                trackingService.trackEvent(TrackingEvent.INTEGRATION_FAILED)
            } else {
                Thread.sleep(RETRY_TIME)
                verifyEndpoint(callback)
            }
        })
        verificationCounter++
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

                trackingService.trackEvent(TrackingEvent.OPEN_DASHBOARD_FROM_PLUGIN)
            } catch (e: Exception) {
                SentryLogger.logException(e)

                trackingService.trackEvent(TrackingEvent.OPEN_DASHBOARD_FROM_PLUGIN_FAILED, buildJsonObject {
                    put("error", e.message ?: "unknown error")
                })
            }
        } ?: SentryLogger.logMessage("cannot open dashboard, embraceProject is null")
    }

    fun getSwazzlerClasspathLine(): String {
        return buildGradleFilesModifier.value?.getClasspathSwazzlerLine() ?: ""
    }

    fun getSwazzlerPluginLine(buildType: BuildType): String {
        return buildGradleFilesModifier.value?.getPluginSwazzlerLine(buildType) ?: ""
    }

    fun sendSupportEmail() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            SentryLogger.logMessage("Tried to contact support")
            val uri = URI("mailto:$CONTACT_EMAIL")
            Desktop.getDesktop().mail(uri)
        }
    }

    fun openBrowser() {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(RESOURCES_LINK))
        }
    }

}
