package io.embrace.android.intellij.plugin.ui.components

import com.intellij.ui.JBColor
import java.awt.Cursor
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import javax.swing.JLabel
import javax.swing.SwingConstants

internal class EmbClickableUnderlinedLabel(text: String, private val action: () -> Unit) : JLabel(text),
    MouseListener,
    MouseMotionListener {
    private var isHoveredOrClicked = false


    init {
        foreground = JBColor.gray
        font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        horizontalAlignment = SwingConstants.CENTER
        addMouseListener(this)
        addMouseMotionListener(this)
        cursor = Cursor(Cursor.HAND_CURSOR)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val fm: FontMetrics = g.getFontMetrics(font)
        val textWidth: Int = fm.stringWidth(text)
        val textHeight: Int = fm.height
        val textX: Int = (width - textWidth) / 2
        val textY: Int = (height - textHeight) / 2 + fm.ascent
        g.drawLine(textX, textY + 2, textX + textWidth, textY + 2)

        foreground = if (isHoveredOrClicked) {
            JBColor.blue
        } else {
            JBColor.gray
        }
    }

    override fun mouseEntered(e: MouseEvent) {
        isHoveredOrClicked = true
        repaint()
    }

    override fun mouseExited(e: MouseEvent) {
        isHoveredOrClicked = false
        repaint()
    }

    override fun mouseClicked(e: MouseEvent) {
        action.invoke()
    }

    override fun mousePressed(e: MouseEvent) {
        isHoveredOrClicked = true
        repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
        isHoveredOrClicked = false
        repaint()
    }

    override fun mouseDragged(e: MouseEvent) {
        // No action required
    }

    override fun mouseMoved(e: MouseEvent) {
        // No action required
    }

}