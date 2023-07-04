package io.embrace.android.intellij.plugin.ui.components

import java.awt.Font
import javax.swing.JButton
import javax.swing.JComponent

internal class EmbButton(text: String, onClick: (JComponent) -> Unit) : JButton(text) {

    init {
        font = Font(Font.SANS_SERIF, Font.PLAIN, 14)

        addActionListener {
            onClick.invoke(this)
        }
    }

}