package io.embrace.android.intellij.plugin.repository


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.repository.network.ApiService
import java.io.File
import java.io.FileWriter

internal class EmbracePluginRepository(
    private val project: Project,
    private val apiService: ApiService = ApiService()
) {
    private val startMethodModifier = StartMethodModifier(project)


    companion object {
        internal const val FILE_ROOT = "file://"
        internal const val MAIN_PATH = "/app/src/main"
        internal const val EMBRACE_CONFIG_FILE = "/embrace-config.json"
        internal const val EMBRACE_SWAZZLER_CLASSPATH = "classpath \"io.embrace:embrace-swazzler:LAST_VERSION\""
        internal const val EMBRACE_SWAZZLER_PLUGIN = "apply plugin: 'embrace-swazzler'"
        internal const val embraceDashboardUrl = ApiService.EMBRACE_DASHBOARD_URL
        internal const val embraceDashboardIntegrationUrl = ApiService.EMBRACE_DASHBOARD_COMPLETE_INTEGRATION
    }

    fun getLastSDKVersion() =
        apiService.getLastSDKVersion()

    fun isConfigurationAlreadyCreated(basePath: String) =
        File(basePath + MAIN_PATH + EMBRACE_CONFIG_FILE).exists()


    fun createEmbraceConfigFile(configFile: String, basePath: String): Boolean {
        try {
            val file = File(basePath + MAIN_PATH + EMBRACE_CONFIG_FILE)

            val writer = FileWriter(file)
            writer.write(configFile)
            writer.close()

            // Refresh the folder containing the new file
            refreshProjectFolder(basePath)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun refreshProjectFolder(basePath: String) {
        val parentFolder = VirtualFileManager.getInstance()
            .findFileByUrl(FILE_ROOT + basePath + MAIN_PATH)

        if (parentFolder != null) {
            ApplicationManager.getApplication().runWriteAction {
                parentFolder.refresh(
                    false,
                    true
                )
            }
        }
    }

    fun getApplicationModules(){
        return
    }

    fun addStartToApplicationClass(callback: StartMethodCallback) =
        startMethodModifier.addStartToApplicationClass(callback)


    fun getProjectName(): String {
        val currentProject = ProjectManager.getInstance().openProjects[0]
        return currentProject.name
    }
}