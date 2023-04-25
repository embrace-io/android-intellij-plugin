package io.embrace.android.intellij.plugin.dataproviders

import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import java.io.File
import java.io.IOException
import java.io.PrintWriter


internal class EmbraceIntegrationDataProvider(
    private val repo: EmbracePluginRepository,
    private val basePath: String?
    ) {

    private val lastEmbraceVersion = repo.getLastSDKVersion()

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

    internal fun createEmbraceFile(appId: String, token: String) {
        basePath?.let { path ->
            val configFile = getResourceAsText("/examplecode/config_template.txt") ?: ""
                .replace("MY_APP_ID", appId)
                .replace("MY_TOKEN", token)

            repo.createEmbraceConfigFile(configFile, path)
        }
    }

    private fun getResourceAsText(path: String): String? =
        object {}.javaClass.getResource(path)?.readText()
}
