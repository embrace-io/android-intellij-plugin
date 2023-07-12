package io.embrace.android.intellij.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindow;
import javax.swing.Icon


internal class EmbraceIntegrationAction : AnAction {

    constructor() : super()

    constructor(text: String, description: String, icon: Icon) : super(text, description, icon)


    override fun actionPerformed(event: AnActionEvent) {
        val toolWindowManager = event.project?.let { ToolWindowManager.getInstance(it) }
        val window: ToolWindow? =
            toolWindowManager?.getToolWindow("  Embrace Assistant") // a left space to set margin with the icon
        window?.show()
    }
}