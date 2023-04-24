package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.embrace.android.intellij.plugin.repository.network.ApiService

class EmbraceIntegrationFactory : ToolWindowFactory {
    private val apiService = ApiService()
    private val repo = EmbracePluginRepository(apiService)
    private val dataProvider = EmbraceIntegrationDataProvider(repo)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = EmbraceIntegrationForm(dataProvider)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(myToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

}