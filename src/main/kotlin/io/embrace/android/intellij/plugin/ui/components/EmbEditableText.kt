package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextField


internal class EmbEditableText(private val hint: String) : JTextField(hint), FocusListener {
    private var showingHint = true
    private val darkGray = Color.decode("#5c5c5c")
    private val viewWidth = 300
    private val viewHeight = 60

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.CENTER_ALIGNMENT

        font = Font("Monospaced", Font.PLAIN, 12)
        isOpaque = true
        background = darkGray
        maximumSize = Dimension(viewWidth, viewHeight)

        addFocusListener(this)
    }


    override fun focusGained(e: FocusEvent?) {
        if (this.text.isEmpty()) {
            super.setText("")
            showingHint = false
        }
    }

    override fun focusLost(e: FocusEvent?) {
        if (this.text.isEmpty()) {
            super.setText(hint)
            showingHint = true
        }
    }

    override fun getText(): String {
        return if (showingHint) "" else super.getText()
    }

    override fun setText(text: String?) {
        super.setText(text)
        showingHint = text.isNullOrBlank()
    }


}