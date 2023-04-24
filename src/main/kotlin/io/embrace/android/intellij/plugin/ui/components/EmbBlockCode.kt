package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea


internal class EmbBlockCode(panel: JPanel, codeExample : String) : JTextArea() {
    private val darkGray = Color.decode("#5c5c5c")
    private val viewWidth = 500

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        font = Font("Monospaced", Font.PLAIN, 12)
        isOpaque = true
        background = darkGray
        maximumSize = Dimension(viewWidth, panel.preferredSize.height)

        border = BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        )

        text = codeExample
    }
}