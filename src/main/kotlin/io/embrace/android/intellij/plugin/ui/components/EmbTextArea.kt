package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JTextArea

internal class EmbTextArea(text: String, textLevel: TextStyle, textColor: Color? = null, step: Steps? = null) :
    JTextArea(text) {

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        lineWrap = true
        wrapStyleWord = true
        isOpaque = false
        border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
        isEditable = false
        font = when (textLevel) {
            TextStyle.HEADLINE_1 -> Font(Font.SANS_SERIF, Font.BOLD, 18)
            TextStyle.HEADLINE_2 -> Font(Font.SANS_SERIF, Font.BOLD, 14)
            TextStyle.HEADLINE_3 -> Font(Font.SANS_SERIF, Font.BOLD, 12)
            TextStyle.BODY -> Font(Font.SANS_SERIF, Font.PLAIN, 12)
        }

        textColor?.let { foreground = textColor }
        step?.let { putClientProperty("step", it) }
    }
}