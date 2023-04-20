package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTextPane

internal class EmbBlockCode(panel : JPanel, block: CodeType) : JTextPane() {
    private val darkGray = Color.decode("#5c5c5c")
    private val viewWidth = 500

    enum class CodeType {
        SWAZZLER,
        SDK,
        START_EMBRACE
    }

    init {
        alignmentX = Component.LEFT_ALIGNMENT
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        isOpaque = true
        background = darkGray
        maximumSize = Dimension(viewWidth, panel.preferredSize.height)


        text = when (block) {
            CodeType.SDK -> getResourceAsText("/examplecode/sdk.txt")
            CodeType.SWAZZLER -> getResourceAsText("/examplecode/swazzler.txt")
            CodeType.START_EMBRACE -> getResourceAsText("/examplecode/sdk.txt")
        }
    }


    private fun getResourceAsText(path: String): String? =
        object {}.javaClass.getResource(path)?.readText()
}