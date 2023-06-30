package io.embrace.android.intellij.plugin.ui.components

import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Color
import java.awt.Component
import java.awt.Image
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel

class FormComponentManager {
    private val errorColor = Color.decode("#d42320")
    private val successColor = Color.decode("#53C541")
    private val icon = ImageIcon(
        ImageIcon(javaClass.classLoader.getResource("icons/check_circle.png")).image.getScaledInstance(
            25,
            25,
            Image.SCALE_SMOOTH
        )
    )
    private val successIcon = JLabel(icon)


    internal val connectEmbraceResultLabel = EmbLabel("", TextStyle.BODY, errorColor)
    internal val connectEmbraceResultPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        alignmentY = Component.CENTER_ALIGNMENT
        alignmentX = Component.LEFT_ALIGNMENT
        add(successIcon)
        add(connectEmbraceResultLabel)
    }

    internal val configFileStatusLabel = EmbLabel("", TextStyle.BODY, errorColor)
    internal val configFileStatusPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        alignmentY = Component.CENTER_ALIGNMENT
        alignmentX = Component.LEFT_ALIGNMENT
        add(successIcon)
        add(configFileStatusLabel)
    }

    internal val gradleResultLabel = EmbLabel("swazzlerAdded".text(), TextStyle.BODY, successColor)
    internal val gradleResultPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        alignmentY = Component.CENTER_ALIGNMENT
        alignmentX = Component.LEFT_ALIGNMENT
        add(successIcon)
        add(gradleResultLabel)
    }


    internal val startResultLabel = EmbLabel("startAddedSuccessfully".text(), TextStyle.BODY, successColor)


}