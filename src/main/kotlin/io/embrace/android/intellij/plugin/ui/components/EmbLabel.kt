package io.embrace.android.intellij.plugin.ui.components

import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JLabel

internal class EmbLabel(text: String, textLevel: TextStyle) : JLabel(text) {

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        border = BorderFactory.createEmptyBorder(10, 0, 0, 0)

        font = when (textLevel) {
            TextStyle.HEADLINE_1 -> Font(Font.SANS_SERIF, Font.BOLD, 18)
            TextStyle.HEADLINE_2 -> Font(Font.SANS_SERIF, Font.BOLD, 14)
            TextStyle.HEADLINE_3 -> Font(Font.SANS_SERIF, Font.BOLD, 12)
            TextStyle.BODY -> Font(Font.SANS_SERIF, Font.PLAIN, 12)
        }
    }

    internal fun setBorder(top: Int, left: Int, bottom: Int, right: Int) {
        border = BorderFactory.createEmptyBorder(top, left, bottom, right)
    }

}