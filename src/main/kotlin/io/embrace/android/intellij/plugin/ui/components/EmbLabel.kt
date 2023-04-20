package io.embrace.android.intellij.plugin.ui.components

import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JLabel

internal class EmbLabel(text: String, textLevel: TEXT_LVL) : JLabel(text) {

    init {

        alignmentX = Component.LEFT_ALIGNMENT
        border = BorderFactory.createEmptyBorder(20, 0, 10, 0)
        font = when (textLevel) {
            TEXT_LVL.HEADLINE_1 -> Font(Font.SANS_SERIF, Font.BOLD, 18)
            TEXT_LVL.HEADLINE_2 -> Font(Font.SANS_SERIF, Font.BOLD, 14)
            TEXT_LVL.HEADLINE_3 -> Font(Font.SANS_SERIF, Font.BOLD, 12)
            TEXT_LVL.BODY -> Font(Font.SANS_SERIF, Font.PLAIN, 12)
        }

    }
}