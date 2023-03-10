package io.embrace.android.intellij.plugin.forms

import com.intellij.openapi.wm.ToolWindow
import javax.swing.JButton
import javax.swing.JPanel

open class MainForm(toolWindow: ToolWindow?) {
    open var openDashboardButton: JButton? = null
    open var content: JPanel? = null
}