package io.embrace.android.intellij.plugin.ui.constants

import com.intellij.ui.JBColor
import javax.swing.UIManager

internal object Colors {
    // regular - dark theme colors
    val panelBackground = JBColor(JBColor.WHITE, UIManager.getColor("Panel.background"))
    val grayBackground = JBColor(JBColor.decode("#ededed"), JBColor.decode("#5c5c5c"))
    val grayText = JBColor(JBColor.decode("#919191"), JBColor.decode("#adadad"))
    val errorColor = JBColor(JBColor.decode("#8B0000"), JBColor.decode("#FF6F6F"))
    val successColor = JBColor(JBColor.decode("#228B22"), JBColor.decode("#90EE90"))

}