package io.embrace.android.intellij.plugin.repository


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import io.embrace.android.intellij.plugin.data.EmbraceProject
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import io.embrace.android.intellij.plugin.repository.network.ApiService
import io.embrace.android.intellij.plugin.repository.sentry.SentryLogger
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
    }

    fun getLastSDKVersion() =
        apiService.getLastSDKVersion()

    fun isConfigurationAlreadyCreated(basePath: String) =
        File(basePath + MAIN_PATH + EMBRACE_CONFIG_FILE).exists()


    fun createEmbraceConfigFile(configFile: String, basePath: String): Boolean {
        return try {
            val path = basePath + MAIN_PATH + EMBRACE_CONFIG_FILE
            val file = File(path)

            val writer = FileWriter(file)
            writer.write(configFile)
            writer.close()

            refreshProjectFolder(path)
            true
        } catch (e: Exception) {
            SentryLogger.logException(e)
            false
        }
    }

    /**
     * It refreshes the directory and then displays the file in the IDE.
     */
    private fun refreshProjectFolder(filePath: String) {
        ApplicationManager.getApplication().invokeLater {
            val virtualFile =
                LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
            if (virtualFile != null) {
                // closes the file and open it again in case that is already being displayed and need to display new changes
                FileEditorManager.getInstance(project).closeFile(virtualFile)
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        }

    }

    fun addStartToApplicationClass(appPackageName: String?, callback: StartMethodCallback) =
        startMethodModifier.addStartToApplicationClass(appPackageName, callback)


    fun getProjectName(): String {
        val currentProject = ProjectManager.getInstance().openProjects[0]
        return currentProject.name
    }

    fun verifyIntegration(embraceProject: EmbraceProject, onSucccess: () -> Unit, onError: () -> Unit) {
        apiService.verifyIntegration(embraceProject, onSucccess, onError)
    }

}