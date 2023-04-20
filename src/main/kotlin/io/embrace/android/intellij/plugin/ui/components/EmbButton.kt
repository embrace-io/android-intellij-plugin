package io.embrace.android.intellij.plugin.ui.components

import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JButton

class EmbButton(tag: String, onClick: () -> Unit) : JButton(tag) {


    init {
        border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
        font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        alignmentX = LEFT_ALIGNMENT

        addActionListener { onClick.invoke() }
    }
}