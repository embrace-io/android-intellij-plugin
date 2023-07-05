package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextField


private const val VIEW_WIDTH = 270
private const val VIEW_HEIGHT = 30

internal class EmbEditableText(private val hint: String? = null, fontSize: Int = 12, step : Steps? = null) : JTextField(hint),
    FocusListener {

    private var showingHint = true
    private val darkGray = Color.decode("#5c5c5c")

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.CENTER_ALIGNMENT

        font = Font("Monospaced", Font.PLAIN, fontSize)
        isOpaque = true
        background = darkGray
        maximumSize = Dimension(VIEW_WIDTH, VIEW_HEIGHT)
        preferredSize = Dimension(VIEW_WIDTH, VIEW_HEIGHT)
        step?.let { putClientProperty("step", it) }

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