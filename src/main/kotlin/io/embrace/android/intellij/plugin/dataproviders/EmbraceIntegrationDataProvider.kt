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
import io.embrace.android.intellij.plugin.repository.sentry.DefaultSentryLogger
import io.embrace.android.intellij.plugin.repository.sentry.SentryLogger
import io.embrace.android.intellij.plugin.ui.components.IntegrationStep
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI

private const val VERIFICATION_COUNT_MAX = 5
private const val RETRY_TIME = 2000L

internal class EmbraceIntegrationDataProvider(
    private val project: Project,
    private val repo: EmbracePluginRepository = EmbracePluginRepository(project),
    private val logger: SentryLogger = DefaultSentryLogger(project, false)
) {
    internal val CONTACT_EMAIL: String = "support@embrace.io"
    private var embraceProject: EmbraceProject? = null
    private var callbackPort: Int = 0
    private val lastEmbraceVersion = repo.getLastSDKVersion()
    internal var applicationModules: List<AppModule>? = null
    private var verificationCounter = 0

    private val buildGradleFilesModifier = lazy {
        project.basePath?.let {
            BuildGradleFilesModifier(project, lastEmbraceVersion, logger)
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
            logger.addAppIdTag(it.appId)
            callback.onOnboardConnected(it.appId, it.token)
        }, {
            logger.logMessage("Error connecting dashboard $it")
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
                logger.logStepCompleted(IntegrationStep.CONFIG_FILE_CREATION)
                callback.onConfigSuccess()
            } else {
                callback.onConfigError("configFileCreationError".text())
            }

        } ?: {
            logger.logMessage("cannot create configuration, project path not found")
            callback.onConfigError("configFilePathNotFoundError".text())
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
                logger.logStepCompleted(IntegrationStep.DEPENDENCY_UPDATE)
                callback.onGradleFilesModifiedSuccessfully()
            }

            rootFileStatus == GradleFileStatus.SWAZZLER_ALREADY_ADDED -> {
                callback.onGradleFileAlreadyModified()
            }

            rootFileStatus == GradleFileStatus.FILE_NOT_FOUND && appFileStatus == GradleFileStatus.FILE_NOT_FOUND -> {
                callback.onGradleFileError("oneOrMoreFilesError".text())
            }

            else -> {
                callback.onGradleFileError("gradleFileError".text())
            }
        }
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
                    logger.logStepCompleted(IntegrationStep.VERIFY_INTEGRATION)
                    callback.onEmbraceIntegrationSuccess()
                }, {
                    if (verificationCounter >= VERIFICATION_COUNT_MAX) {
                        verificationCounter = 0
                        callback.onEmbraceIntegrationError()
                        logger.logMessage("cannot verify integration, max retries reached")
                    } else {
                        Thread.sleep(RETRY_TIME)
                        verifyIntegration(callback)
                    }
                })
                verificationCounter++
            } else {
                callback.onEmbraceIntegrationError()
            }
        } ?: {
            logger.logMessage("cannot verify integration, embraceProject is null")
            callback.onEmbraceIntegrationError()
        }
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
            } catch (e: Exception) {
                logger.logException(e)
            }
        } ?: logger.logMessage("cannot open dashboard, embraceProject is null")
    }

    fun getSwazzlerClasspathLine(): String {
        return buildGradleFilesModifier.value?.getClasspathSwazzlerLine() ?: ""
    }

    fun getSwazzlerPluginLine(buildType: BuildType): String {
        return buildGradleFilesModifier.value?.getPluginSwazzlerLine(buildType) ?: ""
    }

    fun sendSupportEmail() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            logger.logMessage("Tried to contact support")
            val uri = URI("mailto:$CONTACT_EMAIL")
            Desktop.getDesktop().mail(uri)
        }
    }

}
