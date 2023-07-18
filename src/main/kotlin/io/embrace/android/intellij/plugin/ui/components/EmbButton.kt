package io.embrace.android.intellij.plugin.ui.components

import io.embrace.android.intellij.plugin.ui.constants.Colors
import java.awt.Font
import javax.swing.JButton
import javax.swing.JComponent

internal class EmbButton(text: String, step: IntegrationStep? = null, onClick: (JComponent) -> Unit) : JButton(text) {

    init {
        font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        background = Colors.panelBackground
        step?.let { putClientProperty("step", it) }
        addActionListener {
            onClick.invoke(this)
        }
    }

}