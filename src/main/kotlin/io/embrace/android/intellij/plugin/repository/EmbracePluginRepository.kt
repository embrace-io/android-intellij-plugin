package io.embrace.android.intellij.plugin.repository

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFileManager
import io.embrace.android.intellij.plugin.repository.network.ApiService
import java.io.File
import java.io.FileWriter

internal class EmbracePluginRepository {
    private val apiService = ApiService()

    companion object {
        private const val FILE_ROOT = "file://"
        private const val MAIN_PATH = "/app/src/main"
        private const val EMBRACE_CONFIG_FILE = "/embrace-config.json"
    }

    fun getLastSDKVersion() =
        apiService.getLastSDKVersion()

    fun createEmbraceConfigFile(configFile: String, basePath: String): Boolean {
        try {
            val file = File(basePath + MAIN_PATH + EMBRACE_CONFIG_FILE)

            val writer = FileWriter(file)
            writer.write(configFile)
            writer.close()

            // Refresh the folder containing the new file
            val parentFolder = VirtualFileManager.getInstance()
                .findFileByUrl(FILE_ROOT + basePath + MAIN_PATH)
            if (parentFolder != null) {
                ApplicationManager.getApplication().runWriteAction {
                    parentFolder.refresh(
                        false,
                        false
                    )
                }
            }
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


}