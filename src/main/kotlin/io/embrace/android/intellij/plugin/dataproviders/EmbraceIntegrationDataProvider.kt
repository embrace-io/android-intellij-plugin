package io.embrace.android.intellij.plugin.dataproviders

import io.embrace.android.intellij.plugin.dataproviders.callback.ConfigFileCreationCallback
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.net.URI


internal class EmbraceIntegrationDataProvider(
    private val repo: EmbracePluginRepository,
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
            val file = File("$basePath/build.gradle")
            val sb = "classpath \"io.embrace:embrace-swazzler:5.14.0\""
            val writer = PrintWriter(file)
            writer.write(sb)
            writer.close()
        } catch (e: IOException) {
            println("An error occurred reading build.gradle file.")
            e.printStackTrace()
        }
    }


    internal fun createEmbraceFile(
        appId: String,
        token: String,
        callback: ConfigFileCreationCallback,
        shouldOverrideFile: Boolean? = false
    ) {
        basePath?.let {
            val isFileAlreadyCreated = repo.isConfigurationAlreadyCreated(basePath)

            if (isFileAlreadyCreated && shouldOverrideFile == false) {
                callback.onConfigAlreadyExists()
                return
            }

            var configFile = getResourceAsText("/examplecode/config_template.txt") ?: ""
            configFile = configFile.replace("MY_APP_ID", appId)
            configFile = configFile.replace("MY_TOKEN", token)

            if (repo.createEmbraceConfigFile(configFile, basePath))
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
