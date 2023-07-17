package io.embrace.android.intellij.plugin.ui.components

import io.embrace.android.intellij.plugin.ui.constants.Colors
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JTextArea


internal class EmbBlockCode(codeExample: String, step: Steps? = null) : JTextArea(codeExample) {

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        lineWrap = true
        wrapStyleWord = true
        isOpaque = true
        isEditable = false
        font = Font("Monospaced", Font.PLAIN, 12)
        background = Colors.grayBackground

        step?.let { putClientProperty("step", it) }
        border = BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
    }
}