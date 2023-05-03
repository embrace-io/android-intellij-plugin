package io.embrace.android.intellij.plugin.ui.components

import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.SwingConstants

class EmbButton(tag: String, onClick: () -> Unit) : JButton(tag) {


    init {
        alignmentX = LEFT_ALIGNMENT
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.CENTER

        border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
        font = Font(Font.SANS_SERIF, Font.PLAIN, 14)

        addActionListener {
            isEnabled = false
            onClick.invoke()
        }
    }
}