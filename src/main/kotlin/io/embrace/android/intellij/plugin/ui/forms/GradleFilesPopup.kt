package io.embrace.android.intellij.plugin.ui.forms

import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.JBUI
import io.embrace.android.intellij.plugin.data.AppModule
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.ui.constants.Colors
import io.embrace.android.intellij.plugin.utils.extensions.text
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel


internal class GradleFilesPopup(
    dataProvider: EmbraceIntegrationDataProvider,
    applicationModules: List<AppModule>,
    private val yesButtonAction: (AppModule) -> Unit
) : JDialog() {

    companion object {
        private const val POPUP_MIN_WIDTH = 450
        private const val POPUP_MIN_HEIGHT = 300
        private const val smallMargin = 10
    }

    private val popupPanel = JPanel().apply {
        border = BorderFactory.createEmptyBorder(15, 10, 10, 10)
    }

    init {
        popupPanel.layout = GridBagLayout()
        popupPanel.background = Colors.panelBackground
        val constraints = GridBagConstraints()

        constraints.gridx = 0
        constraints.gridy = 0
        constraints.anchor = GridBagConstraints.WEST

        val label = JLabel("targetModules".text())
        popupPanel.add(label, constraints)

        constraints.gridx = 1
        constraints.insets = JBUI.insetsLeft(5)

        val dropdown = ComboBox(applicationModules.map { it.name }.toTypedArray())
        dropdown.selectedIndex = 0
        popupPanel.add(dropdown, constraints)

        constraints.gridx = 0
        constraints.gridy = 1
        constraints.anchor = GridBagConstraints.WEST
        constraints.gridwidth = GridBagConstraints.REMAINDER

        constraints.insets = JBUI.insetsLeft(0)
        constraints.insets = JBUI.insetsTop(15)
        popupPanel.add(JLabel("confirmChanges".text()), constraints)

        constraints.insets = JBUI.insetsTop(25)
        constraints.gridy++
        popupPanel.add(JLabel("projectLevel".text()), constraints)

        constraints.gridy++
        constraints.insets = JBUI.insetsTop(smallMargin)
        val swazzlerLine = JLabel(dataProvider.getSwazzlerClasspathLine())
            .apply {
                background = Colors.grayBackground
                alignmentX = Component.LEFT_ALIGNMENT
                font = Font("Monospaced", Font.BOLD, 12)
                isOpaque = true
                border = BorderFactory.createCompoundBorder(
                    border,
                    BorderFactory.createEmptyBorder(5, 15, 5, 15)
                )
            }

        popupPanel.add(swazzlerLine, constraints)

        constraints.gridy++
        constraints.insets = JBUI.insetsTop(25)
        popupPanel.add(JLabel("appLevelFile".text()), constraints)

        constraints.gridy++
        constraints.insets = JBUI.insetsTop(smallMargin)

        val pluginText =
            JLabel(dataProvider.getSwazzlerPluginLine(applicationModules[dropdown.selectedIndex].type)).apply {
                background = Colors.grayBackground
                alignmentX = Component.LEFT_ALIGNMENT
                font = Font("Monospaced", Font.BOLD, 12)
                isOpaque = true
                border = BorderFactory.createCompoundBorder(
                    border,
                    BorderFactory.createEmptyBorder(5, 15, 5, 15)
                )
            }

        popupPanel.add(pluginText, constraints)

        // Add buttons
        val okButton = JButton("Add")
        okButton.background = Colors.panelBackground
        okButton.addActionListener {
            dispose()
            yesButtonAction.invoke(applicationModules[dropdown.selectedIndex])
        }

        val cancelButton = JButton("Cancel")
        cancelButton.background = Colors.panelBackground
        cancelButton.addActionListener { dispose() }

        val buttonPanel = JPanel()
        buttonPanel.background = Colors.panelBackground
        buttonPanel.add(cancelButton)
        buttonPanel.add(okButton)

        constraints.gridx = 0
        constraints.gridy++
        constraints.anchor = GridBagConstraints.SOUTHEAST
        constraints.gridwidth = GridBagConstraints.REMAINDER
        constraints.insets = JBUI.insetsTop(smallMargin)

        popupPanel.add(buttonPanel, constraints)
    }

    fun showPopup(ideWindow: JComponent?) {
        title = "Modify Gradle Files"
        contentPane.add(popupPanel)
        isResizable = true
        defaultCloseOperation = DISPOSE_ON_CLOSE
        pack()

        minimumSize = Dimension(POPUP_MIN_WIDTH, POPUP_MIN_HEIGHT)

        // Calculate the location relative to the parent frame to display the popup in the middle of the IDE.
        if (ideWindow?.isShowing == true) {
            val ideLocation = ideWindow.locationOnScreen
            val x: Int = ideLocation.x + (ideWindow.width - width) / 2
            val y: Int = ideLocation.y + (ideWindow.height - height) / 2
            setLocation(x, y)
        }

        isVisible = true
    }


}