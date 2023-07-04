package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Image
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

internal class FormComponentManager {

    private val errorColor = Color.decode("#d42320")
    private val successColor = Color.decode("#75D554")

    private val successIcon = ImageIcon(
        ImageIcon(javaClass.classLoader.getResource("icons/check_circle.png")).image.getScaledInstance(
            20,
            20,
            Image.SCALE_SMOOTH
        )
    )

    internal val connectEmbraceResultPanel = getResultLayout().apply { isVisible = false }
    internal val configFileStatusPanel = getResultLayout().apply { isVisible = false }
    internal val gradleResultPanel = getResultLayout().apply { isVisible = false }
    internal val startResultPanel = getResultLayout().apply { isVisible = false }


    private fun getResultLayout(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentY = Component.CENTER_ALIGNMENT
            alignmentX = Component.LEFT_ALIGNMENT

            add(JLabel(successIcon))
            add(EmbLabel("message", TextStyle.BODY).apply {
                border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
            })
        }
    }


    fun changeResultText(panel: JPanel, text: String, success: Boolean = true) {
        panel.isVisible = true
        val icon = panel.getComponent(0)
        val label = panel.getComponent(1)

        if (icon is JLabel && label is JLabel) {
            label.text = text
            icon.isVisible = success

            if (success) {
                label.foreground = successColor
            } else {
                label.foreground = errorColor
            }

        }
    }

}