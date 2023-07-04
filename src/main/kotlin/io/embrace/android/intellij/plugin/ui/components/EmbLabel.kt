package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Font
import javax.swing.JLabel

internal class EmbLabel(text: String, textLevel: TextStyle, textColor: Color? = null) : JLabel(text) {

    init {
        font = when (textLevel) {
            TextStyle.HEADLINE_1 -> Font(Font.SANS_SERIF, Font.BOLD, 18)
            TextStyle.HEADLINE_2 -> Font(Font.SANS_SERIF, Font.BOLD, 14)
            TextStyle.HEADLINE_3 -> Font(Font.SANS_SERIF, Font.BOLD, 12)
            TextStyle.BODY -> Font(Font.SANS_SERIF, Font.PLAIN, 12)
        }

        textColor?.let { foreground = textColor }
    }
}