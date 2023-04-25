package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.embrace.android.intellij.plugin.repository.network.ApiService

class EmbraceIntegrationFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
<<<<<<< HEAD
        val myToolWindow = EmbraceIntegrationForm(project, dataProvider)
=======
        val apiService = ApiService()
        val repo = EmbracePluginRepository(apiService)
        val dataProvider = EmbraceIntegrationDataProvider(repo, project.basePath)
        val myToolWindow = EmbraceIntegrationForm(dataProvider)
>>>>>>> main
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(myToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

}
