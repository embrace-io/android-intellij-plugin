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
import io.embrace.android.intellij.plugin.dataproviders.StartMethodStatus
import io.embrace.android.intellij.plugin.dataproviders.callback.StartMethodCallback
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf.className
import java.nio.file.Files
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
                val applicationNode = manifestXml.documentElement.getElementsByTagName("application").item(0)
                val packageName = manifestXml.documentElement.getAttribute("package")

                return packageName + applicationNode?.attributes?.getNamedItem("android:name")?.nodeValue
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    /**
     * TODO :
     * Si no encuentra la application, mostrar popup de que no
     * Si la encuentra, que se fije si tiene el onCreate. Sino que agregue todo el metodo. Hay otros onCreate?
     * Verificar si ya tiene el import de embrace
     * Si por algo no puede modificar, que abra la clase y muestre error.
     */
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

            ApplicationManager.getApplication().runReadAction {
                val psiFacade = JavaPsiFacade.getInstance(project)
                psiClass = psiFacade.findClass(applicationClass, GlobalSearchScope.allScope(project))
            }

            if (psiClass == null) {
                println("Kotlin class not found: $applicationClass")
                return StartMethodStatus.ERROR
            }

            val kotlinClassPath = psiClass!!.containingFile.virtualFile.path
            val kotlinClassFile = Paths.get(kotlinClassPath)

            if (Files.exists(kotlinClassFile)) {
                val lines = Files.readAllLines(kotlinClassFile)
                val embraceLine = getStartMethodLine(psiClass!!)

                var isEmbraceAdded = false
                var onCreateIndex = -1
                lines.forEachIndexed { index, s ->
                    if (s.contains(embraceLine)) {
                        isEmbraceAdded = true
                        return@forEachIndexed
                    }

                    if (s.contains("super.onCreate")) {
                        onCreateIndex = index
                        return@forEachIndexed
                    }
                }


                if (!isEmbraceAdded) {
                    if (onCreateIndex > 0) {
                        val blankSpaces = lines[onCreateIndex].substringBefore("super.")
                        lines.add(onCreateIndex + 1, "$blankSpaces$embraceLine")
                    } else {
                        // addOnCreate
                    }

                    Files.write(kotlinClassFile, lines)
                    psiClass!!.containingFile.virtualFile.refresh(false, true)
                    return StartMethodStatus.START_ADDED_SUCCESSFULLY
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

        private fun getStartMethodLine(psiClass: PsiClass): String {
            val extension = psiClass.containingFile?.virtualFile?.extension

            return if (extension == "java") {
                "Embrace.getInstance().start(this);"
            } else {
                "Embrace.getInstance().start(this)"
            }
        }
    }
}