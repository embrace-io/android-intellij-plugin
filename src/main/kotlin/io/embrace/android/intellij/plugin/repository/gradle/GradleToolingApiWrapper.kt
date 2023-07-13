package io.embrace.android.intellij.plugin.repository.gradle

import com.android.tools.build.jetifier.core.utils.Log
import io.sentry.Sentry
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
        } catch (e: Exception) {
            Sentry.captureException(e)
            Log.e(TAG, "Error while trying to get build.gradle file: ${e.message}")
        }
        return null
    }

    fun getBuildGradleFilesForModules(selectedModule: String): File? {
        try {
            connector.connect().use { connection ->
                val model = connection.getModel(GradleProject::class.java)
                val modules = model.children
                return modules.first { it?.name.contentEquals(selectedModule) }.buildScript.sourceFile
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
            Log.e(TAG, "Error while trying to get build.gradle file: ${e.message}")
        }
        return null
    }

    fun getModules(): Collection<GradleProject>? {
        try {
            connector.connect().use { connection ->
                val model = connection.getModel(GradleProject::class.java)
                return model.children
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
            Log.e(TAG, "Error while trying to get build.gradle file: ${e.message}")
        }
        return emptyList()
    }

}

private val TAG = GradleToolingApiWrapper::class.simpleName.orEmpty()
