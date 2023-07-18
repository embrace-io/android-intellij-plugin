package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JLabel
import javax.swing.JTextArea


class EmbraceIntegrationFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dataProvider = EmbraceIntegrationDataProvider(project)
        val integrationView = EmbraceIntegrationForm(project, dataProvider)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(integrationView.getContent(), "", false)
        toolWindow.contentManager.addContent(content)

        addResizeEventsListener(toolWindow, integrationView)
    }

    /**
     * This method adds a component listener to adjust the width of the JTextArea components
     * inside a tool window based on the width of the tool window itself.
     * This ensures that the text inside the JTextArea is displayed correctly without any
     * overflow or truncation.
     */
    private fun addResizeEventsListener(toolWindow: ToolWindow, integrationView: EmbraceIntegrationForm) {
        toolWindow.component.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                val maxWidth = toolWindow.component.width // get the available width
                val scrollView = integrationView.panel.components

                for (component in scrollView) {
                    if (component is JTextArea || component is JLabel) {
                        component.maximumSize = Dimension(maxWidth, component.preferredSize.height)
                        component.preferredSize = Dimension(maxWidth, component.preferredSize.height)
                    }
                }
            }
        })
    }

}
