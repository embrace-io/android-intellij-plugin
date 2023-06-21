package io.embrace.android.intellij.plugin.repository

import com.android.tools.idea.projectsystem.getManifestFiles
import com.android.utils.XmlUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import io.embrace.android.intellij.plugin.data.StartMethodStatus
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import org.jetbrains.android.facet.AndroidFacet
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.SwingWorker

internal class StartMethodModifier(private val project: Project) {

    fun addStartToApplicationClass(callback: StartMethodCallback) {
        val applicationClass = getApplicationClass()

        if (applicationClass != null) {
            ManifestManager(applicationClass, project, callback).execute()
        } else {
            callback.onStartStatusUpdated(StartMethodStatus.APPLICATION_CLASS_NOT_FOUND)
        }
    }

    private fun getApplicationClass(): String? {
        val file = VirtualFileManager.getInstance()
            .findFileByUrl(EmbracePluginRepository.FILE_ROOT + project.basePath + EmbracePluginRepository.MAIN_PATH)

        file?.let {
            val module = ModuleUtil.findModuleForFile(it, project) ?: return null
            try {
                val facet = AndroidFacet.getInstance(module)
                val appInfo = facet?.getManifestFiles()?.get(0)

                val manifestXml = XmlUtils.parseDocument(appInfo?.inputStream?.reader(), true)
                val applicationNode = manifestXml.documentElement.getElementsByTagName("application")
                    .item(0).attributes?.getNamedItem("android:name")
                if (applicationNode != null) {
                    val packageName = manifestXml.documentElement.getAttribute("package")
                    return packageName + applicationNode.nodeValue
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    internal class ManifestManager(
        private val applicationClass: String,
        private val project: Project?,
        private val callback: StartMethodCallback
    ) : SwingWorker<StartMethodStatus, Void>() {


        override fun doInBackground(): StartMethodStatus {

            if (project == null) {
                return StartMethodStatus.ERROR
            }

            var psiClass: PsiClass? = null
            var kotlinClassPath: String? = null
            var kotlinClassFile: Path? = null

            ApplicationManager.getApplication().runReadAction {
                val psiFacade = JavaPsiFacade.getInstance(project)
                psiClass = psiFacade.findClass(applicationClass, GlobalSearchScope.allScope(project))
                psiClass?.let {
                    kotlinClassPath = it.containingFile.virtualFile.path
                    kotlinClassFile = Paths.get(kotlinClassPath)
                }
            }

            if (psiClass == null) {
                println("Kotlin class not found: $applicationClass")
                return StartMethodStatus.ERROR
            }

            if (kotlinClassFile != null && Files.exists(kotlinClassFile!!)) {
                val lines = Files.readAllLines(kotlinClassFile)
                val embraceImportLine = getEmbraceImportLine(psiClass!!)
                val embraceStartLine = getStartMethodLine(psiClass!!)

                var isEmbraceImportAdded = false
                var isEmbraceStartAdded = false
                var embraceImportLineIndex = -1
                var embraceStartLineIndex = -1
                lines.forEachIndexed { index, line ->
                    if (line.contains(embraceImportLine)) {
                        isEmbraceImportAdded = true
                        return@forEachIndexed
                    }

                    if (line.contains(embraceStartLine)) {
                        isEmbraceStartAdded = true
                        return@forEachIndexed
                    }

                    if (line.startsWith("import")
                        && index < lines.size - 1
                        && !lines[index + 1].startsWith("import")
                    ) {
                        embraceImportLineIndex = index + 1
                        return@forEachIndexed
                    }

                    if (line.contains("super.onCreate")) {
                        embraceStartLineIndex = index + 1
                        return@forEachIndexed
                    }
                }

                if (!isEmbraceStartAdded) {
                    if (embraceStartLineIndex > 0) {
                        if (!isEmbraceImportAdded && embraceImportLineIndex > 0) {
                            lines.add(embraceImportLineIndex, embraceImportLine)
                            embraceStartLineIndex++
                        }
                        val blankSpaces = lines[embraceStartLineIndex - 1].substringBefore("super.")
                        lines.add(embraceStartLineIndex, "$blankSpaces$embraceStartLine")
                        Files.write(kotlinClassFile, lines)
                        psiClass!!.containingFile.virtualFile.refresh(false, true)
                        return StartMethodStatus.START_ADDED_SUCCESSFULLY
                    } else {
                        return StartMethodStatus.APPLICATION_CLASS_NOT_ON_CREATE
                    }
                } else {
                    return StartMethodStatus.START_ALREADY_ADDED
                }
            } else {
                println("Kotlin class file not found: $kotlinClassPath")
                return StartMethodStatus.ERROR
            }
        }

        override fun done() {
            val result = get() as StartMethodStatus
            callback.onStartStatusUpdated(result)
        }

        private fun getEmbraceImportLine(psiClass: PsiClass): String {
            val extension = psiClass.containingFile?.virtualFile?.extension

            return if (extension == "java") {
                "$EMBRACE_IMPORT_LINE;"
            } else {
                EMBRACE_IMPORT_LINE
            }
        }

        private fun getStartMethodLine(psiClass: PsiClass): String {
            val extension = psiClass.containingFile?.virtualFile?.extension

            return if (extension == "java") {
                "$EMBRACE_START_LINE;"
            } else {
                EMBRACE_START_LINE
            }
        }
    }
}

private const val EMBRACE_IMPORT_LINE = "import io.embrace.android.embracesdk.Embrace"
private const val EMBRACE_START_LINE = "Embrace.getInstance().start(this)"
