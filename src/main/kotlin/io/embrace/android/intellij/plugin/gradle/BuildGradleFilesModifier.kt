package io.embrace.android.intellij.plugin.gradle

import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.idea.gradle.project.build.output.indexOfFirstNonWhitespace
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import io.embrace.android.intellij.plugin.dataproviders.callback.ProjectGradleFileModificationCallback
import io.embrace.android.intellij.plugin.dataproviders.callback.SwazzlerPluginAddedCallback
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.io.File


internal class BuildGradleFilesModifier(
    private val project: Project,
    private val lastEmbraceVersion: String,
    private val gradleAPI: GradleToolingApiWrapper? = project.basePath?.let { GradleToolingApiWrapper(it) }
) {

    private val document = lazy { getGradleDocument() }


//    fun updateAllBuildGradleFiles() {
//        updateBuildGradleFileForProject()
//        updateBuildGradleFileForModules()
//    }


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

    internal fun getBuildGradleFileContent(callback: ProjectGradleFileModificationCallback) {
        document.value?.text?.let { content ->
            val dependenciesIndex = content.indexOf("dependencies {")
            val isEmbraceDependencyAlreadyAdded = content.contains(
                EMBRACE_CLASSPATH_DEPENDENCY.substring(
                    0,
                    EMBRACE_CLASSPATH_DEPENDENCY.indexOf(":")
                )
            )

            if (isEmbraceDependencyAlreadyAdded) {
                callback.onGradleFileError("swazzlerAlreadyAdded".text())
                return
            } else {
                if (dependenciesIndex >= 0) {
                    val firstDependencyIndexWithoutIndent = content.indexOf("\n", dependenciesIndex) + 1
                    val firstDependencyIndexWithIndent =
                        content.indexOfFirstNonWhitespace(firstDependencyIndexWithoutIndent)
                    val indent = content.substring(firstDependencyIndexWithoutIndent, firstDependencyIndexWithIndent)
                    val newDependency = EMBRACE_CLASSPATH_DEPENDENCY.replace("EMBRACE_SDK_VERSION", lastEmbraceVersion)

                    callback.onGradleContentFound(
                        newDependency,
                        content.substring(0, firstDependencyIndexWithIndent) +
                                newDependency +
                                "\n" +
                                indent +
                                content.substring(firstDependencyIndexWithIndent)
                    )
                    return
                }
            }
        }

        callback.onGradleFileError("gradleFileNotFound".text())
    }


    internal fun updateBuildGradleFileForProject(content: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            document.value?.setText(content)
        }
    }


    internal fun addSwazzlerPlugin(callback: SwazzlerPluginAddedCallback) {
        val buildGradleFiles = gradleAPI?.getBuildGradleFilesForModules()

        if (buildGradleFiles.isNullOrEmpty()) {
            Log.e(TAG, "root build.gradle file not found.")
            callback.onSwazzlerPluginError("gradleFileNotFound".text())
            return
        }

        buildGradleFiles.forEach filesLoop@{ file ->
            if (file == null) {
                Log.e(TAG, "build.gradle file not found.")
                return@filesLoop
            }

            val virtualBuildGradleFile = LocalFileSystem.getInstance().findFileByIoFile(file)
            if (virtualBuildGradleFile == null) {
                Log.e(TAG, "build.gradle virtual file not found.")
                return@filesLoop
            }

            val document = FileDocumentManager.getInstance().getDocument(virtualBuildGradleFile)
            if (document == null) {
                Log.e(TAG, "build.gradle document not found.")
                return
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
                return@filesLoop
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
                return@filesLoop
            }

            if (androidApplicationIndexV1 < 0 && androidApplicationIndexV2 < 0) {
                // TODO:("Add support for other types of modules")
                Log.e(TAG, "apply plugin: 'com.android.application' not found on this module.")
                return@filesLoop
            } else {
                callback.onSwazzlerPluginAdded()
            }
        }
    }
}

private val TAG = BuildGradleFilesModifier::class.simpleName.orEmpty()
private const val EMBRACE_CLASSPATH_DEPENDENCY = "classpath \"io.embrace:embrace-swazzler:EMBRACE_SDK_VERSION\""
private const val EMBRACE_APPLY_PLUGIN_V1 = "apply plugin: 'embrace-swazzler'"
private const val EMBRACE_APPLY_PLUGIN_V2 = "id 'embrace-swazzler'"
private const val EMBRACE_SDK_DEPENDENCY = "implementation \"io.embrace:embrace-android-sdk:EMBRACE_SDK_VERSION\""
