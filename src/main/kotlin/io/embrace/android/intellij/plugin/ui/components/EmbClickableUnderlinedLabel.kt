package io.embrace.android.intellij.plugin.ui.components

import java.awt.Component
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel


internal class EmbClickableUnderlinedLabel(
    text: String,
    private val action: () -> Unit
) : JLabel(text) {

    init {
        font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        alignmentX = Component.LEFT_ALIGNMENT
        cursor = Cursor(Cursor.HAND_CURSOR)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                action.invoke()
            }
        })
    }

}