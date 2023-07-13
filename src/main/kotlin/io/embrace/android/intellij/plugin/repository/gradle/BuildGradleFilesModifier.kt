package io.embrace.android.intellij.plugin.repository.gradle

import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.idea.gradle.project.build.output.indexOfFirstNonWhitespace
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil.addDependency
import com.intellij.openapi.vfs.LocalFileSystem
import io.embrace.android.intellij.plugin.data.AppModule
import io.embrace.android.intellij.plugin.data.GradleFileStatus
import io.embrace.android.intellij.plugin.data.BuildType
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.sentry.Sentry
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
        try {
            appGradleFile.value?.text?.let { content ->
                val dependenciesIndex = content.indexOf("dependencies")
                val isEmbraceDependencyAlreadyAdded = content.contains("embrace-swazzler")

                if (isEmbraceDependencyAlreadyAdded) {
                    return GradleFileStatus.SWAZZLER_ALREADY_ADDED
                } else {
                    if (dependenciesIndex >= 0) {
                        val newFile = addDependency(dependenciesIndex, content)

                        WriteCommandAction.runWriteCommandAction(project) {
                            appGradleFile.value?.setText(newFile)
                        }
                        return GradleFileStatus.ADDED_SUCCESSFULLY

                    } else {
                        // here we should add the dependencies block as well.
                    }
                }
            } ?: return GradleFileStatus.FILE_NOT_FOUND
        } catch (e: Exception) {
            Sentry.captureException(e)
            GradleFileStatus.ERROR
        }
        return GradleFileStatus.ERROR
    }

    private fun addDependency(dependenciesIndex: Int, content: String): String {
        val firstDependencyIndexWithoutIndent = content.indexOf("\n", dependenciesIndex) + 1
        val firstDependencyIndexWithIndent =
            content.indexOfFirstNonWhitespace(firstDependencyIndexWithoutIndent)
        val indent = content.substring(firstDependencyIndexWithoutIndent, firstDependencyIndexWithIndent)

        val newDependency =
            if (content.replace(" ", "").contains("classpath(")) {
                EmbracePluginRepository.EMBRACE_SWAZZLER_CLASSPATH_V2.replace(
                    "LAST_VERSION",
                    lastEmbraceVersion
                )
            } else {
                EmbracePluginRepository.EMBRACE_SWAZZLER_CLASSPATH.replace(
                    "LAST_VERSION",
                    lastEmbraceVersion
                )
            }


        return content.substring(0, firstDependencyIndexWithIndent) +
                newDependency +
                "\n" +
                indent +
                content.substring(firstDependencyIndexWithIndent)
    }

    internal fun getModules(): Collection<GradleProject>? {
        return gradleAPI?.getModules()
    }

    internal fun getApplicationModules(modules: Collection<GradleProject>?): List<AppModule> {

        if (modules.isNullOrEmpty()) {
            Log.e(TAG, "root build.gradle file not found.")
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

            if (document.text.contains("com.android.application")) {
                if (document.text.contains("apply plugin:")) {
                    applicationModules.add(AppModule(module.name, BuildType.GROOVY_V1))
                } else if (document.text.contains("id(") || document.text.contains("id (")) {
                    applicationModules.add(AppModule(module.name, BuildType.KOTLIN_DSL))
                } else {
                    applicationModules.add(AppModule(module.name, BuildType.GROOVY_V2))
                }
            }

        }

        return applicationModules
    }

    internal fun addSwazzlerPlugin(selectedModule: AppModule): GradleFileStatus {
        val file = gradleAPI?.getBuildGradleFilesForModules(selectedModule.name)

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


        val androidApplicationIndex = content.replace('\"', '\'').indexOf("com.android.application")
        val isSwazzlerAlreadyApplied = content.contains(selectedModule.type.swazzler)

        if (androidApplicationIndex >= 0 && !isSwazzlerAlreadyApplied) {
            val newLineIndex = content.indexOf("\n", androidApplicationIndex) + 1
            val newLineWithIntentIndex = content.indexOfFirstNonWhitespace(newLineIndex)
            val indent = content.substring(newLineIndex, newLineWithIntentIndex)
            content = content.substring(0, newLineWithIntentIndex) +
                    selectedModule.type.swazzler +
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

    fun syncGradle(project: Project) {
        try {
            ExternalSystemUtil.refreshProjects(ImportSpecBuilder(project, ProjectSystemId("GRADLE")))
        } catch (e: Exception) {
            Sentry.captureException(e)
            e.printStackTrace()
        }
    }
}

private val TAG = BuildGradleFilesModifier::class.simpleName.orEmpty()

