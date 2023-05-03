package io.embrace.android.intellij.plugin.gradle

import com.android.tools.build.jetifier.core.utils.Log
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.GradleProject
import java.io.File

class GradleToolingApiWrapper(basePath: String) {
    private val connector: GradleConnector = GradleConnector.newConnector()

    init {
        connector.forProjectDirectory(File(basePath))
    }

    fun getBuildGradleFileForProject(): File? {
        try {
            connector.connect().use { connection ->
                val model = connection.getModel(GradleProject::class.java)
                val buildScript = model.buildScript
                return buildScript.sourceFile
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "Error while trying to get build.gradle file: ${e.message}")
        }
        return null
    }

    fun getBuildGradleFilesForModules(): List<File?> {
        try {
            connector.connect().use { connection ->
                val model = connection.getModel(GradleProject::class.java)
                val modules = model.children
                return modules.map { it.buildScript.sourceFile }
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "Error while trying to get build.gradle file: ${e.message}")
        }
        return emptyList()
    }
}
private val TAG = GradleToolingApiWrapper::class.simpleName.orEmpty()
