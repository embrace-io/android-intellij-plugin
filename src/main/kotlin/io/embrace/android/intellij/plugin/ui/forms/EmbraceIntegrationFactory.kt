package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.repository.EmbracePluginRepository
import io.embrace.android.intellij.plugin.repository.network.ApiService
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JLabel
import javax.swing.JTextArea


class EmbraceIntegrationFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val apiService = ApiService()
        val repo = EmbracePluginRepository(apiService)
        val dataProvider = EmbraceIntegrationDataProvider(repo, project.basePath)

        val myToolWindow = EmbraceIntegrationForm(dataProvider)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(myToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)

        // Add a ComponentListener to the ToolWindow to listen for resize events
        toolWindow.component.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                val maxWidth = toolWindow.component.width // get the available width
                val components: Array<Component> =
                    myToolWindow.getContent().components // get the components of the panel

                for (component in components) {
                    if (component is JTextArea) {
                        component.maximumSize = Dimension(maxWidth, component.preferredSize.height)
                    }
                }
            }
        })
    }

}
