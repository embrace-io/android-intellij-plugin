package io.embrace.android.intellij.plugin.repository

import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.idea.projectsystem.getManifestFiles
import com.android.utils.XmlUtils
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.android.facet.AndroidFacet
import java.io.File

class StartMethodModifier(private val project: Project) {




    fun getApplicationClass(): String? {
        val file = VirtualFileManager.getInstance()
            .findFileByUrl(EmbracePluginRepository.FILE_ROOT + project.basePath + EmbracePluginRepository.MAIN_PATH)

        file?.let {
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
        }

        return null
    }

}