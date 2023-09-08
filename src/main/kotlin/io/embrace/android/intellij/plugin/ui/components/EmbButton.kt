package io.embrace.android.intellij.plugin.ui.components

import io.embrace.android.intellij.plugin.ui.constants.Colors
import java.awt.Dimension
import java.awt.Font
import javax.swing.JButton
import javax.swing.JComponent

internal class EmbButton(text: String, step: IntegrationStep? = null, onClick: (JComponent) -> Unit) : JButton(text) {

    init {
        font = Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE)
        background = Colors.panelBackground
        preferredSize = Dimension(preferredSize.width, BTN_PREFERRED_HEIGHT)
        minimumSize = Dimension(minimumSize.width, BTN_PREFERRED_HEIGHT)
        maximumSize = Dimension(maximumSize.width, BTN_PREFERRED_HEIGHT)
        step?.let { putClientProperty("step", it) }
        addActionListener {
            onClick.invoke(this)
        }
    }
}

private const val FONT_SIZE = 14
private const val BTN_PREFERRED_HEIGHT = 35
