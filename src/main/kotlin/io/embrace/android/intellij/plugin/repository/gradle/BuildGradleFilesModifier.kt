package io.embrace.android.intellij.plugin.repository.gradle

import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.idea.gradle.project.build.output.indexOfFirstNonWhitespace
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.embrace.android.intellij.plugin.data.AppModule
import io.embrace.android.intellij.plugin.data.BuildType
import io.embrace.android.intellij.plugin.data.GradleFileStatus
import io.embrace.android.intellij.plugin.repository.sentry.SentryLogger
import org.gradle.tooling.model.GradleProject
import java.io.File

internal const val EMBRACE_SWAZZLER_CLASSPATH = "classpath \"io.embrace:embrace-swazzler:LAST_VERSION\""
internal const val EMBRACE_SWAZZLER_CLASSPATH_KOTLIN = "classpath (\"io.embrace:embrace-swazzler:LAST_VERSION\")"

internal class BuildGradleFilesModifier(
    private val project: Project,
    private val lastEmbraceVersion: String,
    private val gradleAPI: GradleToolingApiWrapper? = project.basePath?.let { GradleToolingApiWrapper(it) }
) {
    private var isKotlinFile = false
    internal var appPackageName: String? = null

    internal fun getModules(): Collection<GradleProject>? {
        return gradleAPI?.getModules()
    }

    internal fun getApplicationModules(modules: Collection<GradleProject>?): List<AppModule> {

        if (modules.isNullOrEmpty()) {
            Log.e(TAG, "root build.gradle file not found.")
            SentryLogger.logMessage("root build.gradle file not found.")
            return emptyList()
        }

        val applicationModules = mutableListOf<AppModule>()

        modules.forEach { module ->
            val file = module.buildScript.sourceFile

            if (file == null) {
                Log.e(TAG, "build.gradle file not found.")
                return@forEach
            }

            val virtualBuildGradleFile = LocalFileSystem.getInstance().findFileByIoFile(file)
            if (virtualBuildGradleFile == null) {
                Log.e(TAG, "build.gradle virtual file not found.")
                return@forEach
            }

            val document = FileDocumentManager.getInstance().getDocument(virtualBuildGradleFile)
            if (document == null) {
                Log.e(TAG, "build.gradle document not found.")
                return@forEach
            }

            isKotlinFile = file.name.endsWith(".kts")
            if (document.text.contains("com.android.application")) {
                if (document.text.contains("apply plugin")) {
                    applicationModules.add(AppModule(module.name, BuildType.APPLY_PLUGIN))
                } else if (document.text.contains("id")) {
                    if (isKotlinFile) {
                        applicationModules.add(AppModule(module.name, BuildType.PLUGIN_ID_KOTLIN))
                    } else {
                        applicationModules.add(AppModule(module.name, BuildType.PLUGIN_ID_GROOVY))
                    }
                } else {
                    SentryLogger.logMessage("build type not identified")
                    applicationModules.add(AppModule(module.name, BuildType.NOT_IDENTIFY))
                }
            } else {
                SentryLogger.logMessage("Android Plugin not found")
            }

        }

        return applicationModules
    }


    // ---- CLASSPATH ------ \\

    internal fun addSwazzlerClasspath(): GradleFileStatus {
        return try {
            val buildGradleFile = gradleAPI?.getBuildGradleFileForProject() ?: File(project.basePath + "/build.gradle")

            if (!buildGradleFile.exists()) {
                Log.e(TAG, "root build.gradle file not found.")
                return GradleFileStatus.FILE_NOT_FOUND
            }

            val virtualBuildGradleFile = LocalFileSystem.getInstance().findFileByIoFile(buildGradleFile)
                ?: return GradleFileStatus.FILE_NOT_FOUND
            val document = FileDocumentManager.getInstance().getDocument(virtualBuildGradleFile)
                ?: return GradleFileStatus.FILE_NOT_FOUND
            val content = document.text

            if (content.contains("embrace-swazzler")) {
                return GradleFileStatus.SWAZZLER_ALREADY_ADDED
            }

            val dependenciesStartRegEx = "dependencies\\s*\\n*\\{".toRegex()
            var dependenciesIndex = dependenciesStartRegEx.find(content)?.range?.first ?: -1
            var newContent = content

            if (dependenciesIndex <= 0) {
                newContent = addDependenciesBlock(content)
                dependenciesIndex = dependenciesStartRegEx.find(newContent)?.range?.first ?: -1
            }

            newContent = addClasspath(dependenciesIndex, newContent)

            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(newContent)
            }

            openAndRefreshFile(virtualBuildGradleFile)
            GradleFileStatus.ADDED_SUCCESSFULLY
        } catch (e: Exception) {
            SentryLogger.logException(e)
            GradleFileStatus.ERROR
        }
    }

    private fun addDependenciesBlock(content: String): String {
        val buildScriptTag = "buildscript"
        val dependenciesTag = "dependencies"

        val buildScriptBlockRegEx = "$buildScriptTag\\s*\\n*\\{".toRegex()
        val buildScriptIndex = buildScriptBlockRegEx.find(content)?.range?.first ?: -1

        return if (buildScriptIndex > 0) {
            val braceIndex = content.indexOf('{', startIndex = buildScriptIndex)

            if (braceIndex != -1) {
                val part1 = content.substring(0, braceIndex + 1)
                val part2 = content.substring(braceIndex + 1)

                "$part1\n    $dependenciesTag {\n    }\n$part2"
            } else {
                content
            }
        } else {
            "$buildScriptTag {\n    $dependenciesTag {\n    }\n}\n$content"
        }
    }

    private fun addClasspath(dependenciesIndex: Int, content: String): String {
        val firstDependencyIndexWithoutIndent = content.indexOf("\n", dependenciesIndex) + 1
        val firstDependencyIndexWithIndent =
            content.indexOfFirstNonWhitespace(firstDependencyIndexWithoutIndent)
        var indent = content.substring(firstDependencyIndexWithoutIndent, firstDependencyIndexWithIndent)

        if (indent.isEmpty()) {
            indent = "    "
        }

        val newDependency = getClasspathSwazzlerLine()

        return content.substring(0, firstDependencyIndexWithIndent) +
                newDependency +
                "\n" +
                indent +
                content.substring(firstDependencyIndexWithIndent)
    }

    internal fun getClasspathSwazzlerLine(): String {
        val swazzlerClasspath = if (isKotlinFile) {
            EMBRACE_SWAZZLER_CLASSPATH_KOTLIN
        } else {
            EMBRACE_SWAZZLER_CLASSPATH
        }

        return swazzlerClasspath.replace("LAST_VERSION", lastEmbraceVersion)
    }


    // ---- PLUGIN ------ \\


    internal fun addSwazzlerPlugin(selectedModule: AppModule): GradleFileStatus {
        val file = gradleAPI?.getBuildGradleFilesForModules(selectedModule.name)
            ?: run {
                Log.e(TAG, "root build.gradle file not found.")
                return GradleFileStatus.ERROR
            }

        val virtualBuildGradleFile = LocalFileSystem.getInstance().findFileByIoFile(file)
            ?: run {
                Log.e(TAG, "build.gradle virtual file not found.")
                return GradleFileStatus.ERROR
            }

        val document = FileDocumentManager.getInstance().getDocument(virtualBuildGradleFile)
            ?: run {
                Log.e(TAG, "build.gradle document not found.")
                return GradleFileStatus.ERROR
            }

        val content = document.text

        saveAppPackageName(content)

        val androidApplicationIndex = content.indexOf("com.android.application").takeIf { it >= 0 }
            ?: return GradleFileStatus.PLUGINS_BLOCK_NOT_FOUND

        if (content.contains("embrace-swazzler")) {
            return GradleFileStatus.SWAZZLER_ALREADY_ADDED
        }

        val newContent = addPlugin(content, androidApplicationIndex, selectedModule.type)

        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(newContent)
        }

        openAndRefreshFile(virtualBuildGradleFile)
        return GradleFileStatus.ADDED_SUCCESSFULLY
    }


    /**
     * Save the app package name to be used later when adding the start method.
     * It is contained in the build.gradle file as "applicationId" or "namespace".
     */
    private fun saveAppPackageName(content: String) {
        val packageNamePrefixes = listOf("applicationId", "namespace")
        val packageNamePrefix = packageNamePrefixes.find { content.contains(it) }

        packageNamePrefix?.let {
            val startIndex = content.indexOf(it)
            val lineEndIndex = content.indexOf("\n", startIndex)
            val line = content.substring(startIndex, lineEndIndex)

            appPackageName = line
                .replace(it, "")
                .replace("=", "")
                .replace("\"", "")
                .trim()
        }
    }

    private fun addPlugin(content: String, androidApplicationIndex: Int, type: BuildType): String {
        val newLineIndex = content.indexOf("\n", androidApplicationIndex) + 1
        val newLineWithIntentIndex = content.indexOfFirstNonWhitespace(newLineIndex)
        val indent = content.substring(newLineIndex, newLineWithIntentIndex)

        return content.substring(0, newLineWithIntentIndex) +
                getPluginSwazzlerLine(type) +
                "\n" +
                indent +
                content.substring(newLineWithIntentIndex)
    }


    internal fun getPluginSwazzlerLine(type: BuildType): String {
        return when (type) {
            BuildType.APPLY_PLUGIN -> "apply plugin: 'embrace-swazzler'"
            BuildType.PLUGIN_ID_KOTLIN -> "id (\"embrace-swazzler\")"
            BuildType.PLUGIN_ID_GROOVY -> "id 'embrace-swazzler'"
            else -> "id (\"embrace-swazzler\")"
        }
    }


    fun syncGradle(project: Project) {
        try {
            ExternalSystemUtil.refreshProjects(ImportSpecBuilder(project, ProjectSystemId("GRADLE")))
        } catch (e: Exception) {
            SentryLogger.logException(e)
            e.printStackTrace()
        }
    }

    /**
     * It refreshes the directory and then displays the file in the IDE.
     */
    private fun openAndRefreshFile(virtualFile: VirtualFile) {
        try {
            ApplicationManager.getApplication().invokeLater {
                FileEditorManager.getInstance(project).closeFile(virtualFile)
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        } catch (ex: Exception) {
            // ignore
        }
    }

    fun retrievePackageName() {
        try {
            val file = gradleAPI?.getBuildGradleFilesForModules()
                ?: run {
                    Log.e(TAG, "root build.gradle file not found.")
                    return
                }

            val virtualBuildGradleFile = LocalFileSystem.getInstance().findFileByIoFile(file)
                ?: run {
                    Log.e(TAG, "build.gradle virtual file not found.")
                    return
                }

            val document = FileDocumentManager.getInstance().getDocument(virtualBuildGradleFile)
                ?: run {
                    Log.e(TAG, "build.gradle document not found.")
                    return
                }

            val content = document.text
            saveAppPackageName(content)
        } catch (e: Exception) {
            SentryLogger.logException(e)
        }
    }
}

private val TAG = BuildGradleFilesModifier::class.simpleName.orEmpty()

