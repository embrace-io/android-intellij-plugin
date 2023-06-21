package io.embrace.android.intellij.plugin.repository.gradle

import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.idea.gradle.project.build.output.indexOfFirstNonWhitespace
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import io.embrace.android.intellij.plugin.dataproviders.GradleFileStatus
import org.gradle.tooling.model.GradleProject
import java.io.File


internal class BuildGradleFilesModifier(
    private val project: Project,
    private val lastEmbraceVersion: String,
    private val gradleAPI: GradleToolingApiWrapper? = project.basePath?.let { GradleToolingApiWrapper(it) }
) {

    private val appGradleFile = lazy { getGradleDocument() }

    private fun getGradleDocument(): Document? {
        val buildGradleFile = gradleAPI?.getBuildGradleFileForProject() ?: File(project.basePath + "/build.gradle")
        if (!buildGradleFile.exists()) {
            Log.e(TAG, "root build.gradle file not found.")
            return null
        }

        val virtualBuildGradleFile = LocalFileSystem.getInstance().findFileByIoFile(buildGradleFile)
        if (virtualBuildGradleFile == null) {
            Log.e(TAG, "root build.gradle virtual file not found.")
            return null
        }

        val document = FileDocumentManager.getInstance().getDocument(virtualBuildGradleFile)
        if (document == null) {
            Log.e(TAG, "root build.gradle document not found.")
            return null
        }

        return document
    }

    internal fun updateBuildGradleFileContent(): GradleFileStatus {
        appGradleFile.value?.text?.let { content ->
            val dependenciesIndex = content.indexOf("dependencies {")
            val isEmbraceDependencyAlreadyAdded = content.contains("embrace-swazzler")

            if (isEmbraceDependencyAlreadyAdded) {
                return GradleFileStatus.SWAZZLER_ALREADY_ADDED
            } else {
                if (dependenciesIndex >= 0) {
                    val firstDependencyIndexWithoutIndent = content.indexOf("\n", dependenciesIndex) + 1
                    val firstDependencyIndexWithIndent =
                        content.indexOfFirstNonWhitespace(firstDependencyIndexWithoutIndent)
                    val indent = content.substring(firstDependencyIndexWithoutIndent, firstDependencyIndexWithIndent)
                    val newDependency = EMBRACE_CLASSPATH_DEPENDENCY.replace("EMBRACE_SDK_VERSION", lastEmbraceVersion)

                    val newFile = content.substring(0, firstDependencyIndexWithIndent) +
                            newDependency +
                            "\n" +
                            indent +
                            content.substring(firstDependencyIndexWithIndent)

                    return try {
                        WriteCommandAction.runWriteCommandAction(project) {
                            appGradleFile.value?.setText(newFile)
                        }
                        GradleFileStatus.ADDED_SUCCESSFULLY
                    } catch (e: Exception) {
                        GradleFileStatus.ERROR
                    }
                }
            }
        } ?: return GradleFileStatus.FILE_NOT_FOUND

        return GradleFileStatus.ERROR
    }

    internal fun getModules(): Collection<GradleProject>? {
        return gradleAPI?.getModules()
    }

    internal fun getApplicationModules(modules : Collection<GradleProject>?): List<String> {

        if (modules.isNullOrEmpty()) {
            Log.e(TAG, "root build.gradle file not found.")
            return emptyList()
        }

        val applicationModules = mutableListOf<String>()

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

            if (document.text.contains("com.android.application")) {
                applicationModules.add(module.name)
            }

        }

        return applicationModules
    }

    internal fun addSwazzlerPlugin(selectedModule: String): GradleFileStatus {
        val file = gradleAPI?.getBuildGradleFilesForModules(selectedModule)

        if (file == null) {
            Log.e(TAG, "root build.gradle file not found.")
            return GradleFileStatus.ERROR
        }

        val virtualBuildGradleFile = LocalFileSystem.getInstance().findFileByIoFile(file)
        if (virtualBuildGradleFile == null) {
            Log.e(TAG, "build.gradle virtual file not found.")
            return GradleFileStatus.ERROR
        }

        val document = FileDocumentManager.getInstance().getDocument(virtualBuildGradleFile)
        if (document == null) {
            Log.e(TAG, "build.gradle document not found.")
            return GradleFileStatus.ERROR
        }

        var content = document.text

        val androidApplicationIndexV1 = content.indexOf("apply plugin: 'com.android.application'")
        val isAndroidApplicationV1AlreadyApplied = content.contains(EMBRACE_APPLY_PLUGIN_V1)
        if (androidApplicationIndexV1 >= 0 && !isAndroidApplicationV1AlreadyApplied) {
            val newLineIndex = content.indexOf("\n", androidApplicationIndexV1) + 1

            content = content.substring(0, newLineIndex) +
                    EMBRACE_APPLY_PLUGIN_V1 +
                    "\n" +
                    content.substring(newLineIndex)

            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(content)
            }

            return GradleFileStatus.ADDED_SUCCESSFULLY
        }

        val androidApplicationIndexV2 = content.indexOf("id 'com.android.application'")
        val isAndroidApplicationV2AlreadyApplied = content.contains(EMBRACE_APPLY_PLUGIN_V2)
        if (androidApplicationIndexV2 >= 0 && !isAndroidApplicationV2AlreadyApplied) {
            val newLineIndex = content.indexOf("\n", androidApplicationIndexV2) + 1
            val newLineWithIntentIndex = content.indexOfFirstNonWhitespace(newLineIndex)
            val indent = content.substring(newLineIndex, newLineWithIntentIndex)
            content = content.substring(0, newLineWithIntentIndex) +
                    EMBRACE_APPLY_PLUGIN_V2 +
                    "\n" +
                    indent +
                    content.substring(newLineWithIntentIndex)

            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(content)
            }

            return GradleFileStatus.ADDED_SUCCESSFULLY
        }

        return GradleFileStatus.ERROR
    }
}

private val TAG = BuildGradleFilesModifier::class.simpleName.orEmpty()
private const val EMBRACE_CLASSPATH_DEPENDENCY = "classpath \"io.embrace:embrace-swazzler:EMBRACE_SDK_VERSION\""
private const val EMBRACE_APPLY_PLUGIN_V1 = "apply plugin: 'embrace-swazzler'"
private const val EMBRACE_APPLY_PLUGIN_V2 = "id 'embrace-swazzler'"
private const val EMBRACE_SDK_DEPENDENCY = "implementation \"io.embrace:embrace-android-sdk:EMBRACE_SDK_VERSION\""
