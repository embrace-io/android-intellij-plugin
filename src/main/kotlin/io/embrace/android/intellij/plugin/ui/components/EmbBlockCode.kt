package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea

private const val VIEW_WIDTH = 500

internal class EmbBlockCode(panel: JPanel, codeExample: String, step: Steps? = null) : JTextArea(codeExample) {
    private val darkGray = Color.decode("#5c5c5c")

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        font = Font("Monospaced", Font.PLAIN, 12)
        isOpaque = true
        background = darkGray
        maximumSize = Dimension(VIEW_WIDTH, panel.preferredSize.height)
        step?.let { putClientProperty("step", it) }
        border = BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        )
    }
}