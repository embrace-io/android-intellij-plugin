package io.embrace.android.intellij.plugin.repository

import com.android.tools.idea.projectsystem.getManifestFiles
import com.android.utils.XmlUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import io.embrace.android.intellij.plugin.repository.network.ApiService
import org.jetbrains.android.facet.AndroidFacet
import java.io.File
import java.io.FileWriter

internal class EmbracePluginRepository(private val apiService: ApiService) {
    val embraceDashboardUrl = ApiService.EMBRACE_DASHBOARD_URL

    companion object {
        private const val FILE_ROOT = "file://"
        private const val MAIN_PATH = "/app/src/main"
        private const val EMBRACE_CONFIG_FILE = "/embrace-config.json"
    }

    fun getLastSDKVersion() =
        apiService.getLastSDKVersion()

    fun isConfigurationAlreadyCreated(basePath: String) =
        File(basePath + MAIN_PATH + EMBRACE_CONFIG_FILE).exists()


    fun createEmbraceConfigFile(configFile: String, basePath: String): Boolean {
        try {
            val file = File(basePath + MAIN_PATH + EMBRACE_CONFIG_FILE)

            val writer = FileWriter(file)
            writer.write(configFile)
            writer.close()

            // Refresh the folder containing the new file
            refreshProjectFolder(basePath)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun refreshProjectFolder(basePath: String) {
        val parentFolder = VirtualFileManager.getInstance()
            .findFileByUrl(FILE_ROOT + basePath + MAIN_PATH)

        if (parentFolder != null) {
            ApplicationManager.getApplication().runWriteAction {
                parentFolder.refresh(
                    false,
                    true
                )
            }
        }
    }

    fun getApplicationClass(project: Project?): String? {
        val file = VirtualFileManager.getInstance()
            .findFileByUrl(FILE_ROOT + project?.basePath + MAIN_PATH)

        file?.let {
            project?.let { project ->
                val module = ModuleUtil.findModuleForFile(it, project) ?: return null
                try {
                    val facet = AndroidFacet.getInstance(module)
                    val appInfo = facet?.getManifestFiles()?.get(0)

                    val manifestXml = XmlUtils.parseDocument(appInfo?.inputStream?.reader(), true)
                    val applicationNode = manifestXml.documentElement.getElementsByTagName("application").item(0)

                    return applicationNode?.attributes?.getNamedItem("android:name")?.nodeValue
                } catch (e: Exception) {
                    println("An error occurred reading build.gradle file.")
                    e.printStackTrace()
                }
                return null
            }
        } ?: return null
    }

    fun addEmbraceStartToApplicationClass(applicationClass: String, project: Project?) {
        var psiClass: PsiClass? = null

        project?.let {
            // Get the JavaPsiFacade instance for the project
            val psiFacade = JavaPsiFacade.getInstance(it)

            // Get the PsiClass for the given fully qualified name
            psiClass = psiFacade.findClass(applicationClass, GlobalSearchScope.allScope(it))

        } ?: return

        val myClass: PsiClass = psiClass ?: return

        val runnable = object : Runnable {
            override fun run() {
                // Find the onCreate() method in the class
                val onCreateMethod = myClass.findMethodsByName("onCreate", false).firstOrNull()
                    ?: return // return if onCreate() method not found

                // Get the body of the onCreate() method
                val onCreateBody = onCreateMethod.body ?: return // return if onCreate() method has no body

                // Find the super.onCreate() statement in the method body
                val superOnCreateStatement =
                    PsiTreeUtil.findChildrenOfType(onCreateBody, PsiMethodCallExpression::class.java)
                        .find { it.text == "super.onCreate()" }
                        ?: return // return if super.onCreate() statement not found

                // Find the next statement after super.onCreate() statement TODO check its functionality
                val nextStatement = superOnCreateStatement.nextSibling

                // Create a new statement to be added
                val statementText = "Embrace.getInstance().start(this, false)"
                val newStatement = PsiElementFactory.getInstance(project)
                    .createStatementFromText(statementText, null)

                onCreateMethod.addAfter(newStatement, superOnCreateStatement)
            }
        }
        WriteCommandAction.runWriteCommandAction(project, runnable)
    }

}