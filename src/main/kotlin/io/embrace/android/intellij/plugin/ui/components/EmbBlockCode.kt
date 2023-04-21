package io.embrace.android.intellij.plugin.ui.components


import io.embrace.android.intellij.plugin.constants.CodeType
import io.embrace.android.intellij.plugin.dataproviders.EmbraceIntegrationDataProvider
import io.embrace.android.intellij.plugin.network.HttpService

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea


internal class EmbBlockCode(panel: JPanel, block: CodeType, httpService: HttpService) : JTextArea() {
    private val embraceIntegrationDataProvider = EmbraceIntegrationDataProvider(httpService)

    private val darkGray = Color.decode("#5c5c5c")
    private val viewWidth = 500

    init {
//        contentType = "text/html"
        alignmentX = Component.LEFT_ALIGNMENT
        font = Font("Monospaced", Font.PLAIN, 12)
        isOpaque = true
        background = darkGray
        maximumSize = Dimension(viewWidth, panel.preferredSize.height)

        border = BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );


        val code = when (block) {
            CodeType.SDK -> getResourceAsText("/examplecode/sdk.txt")
            CodeType.SWAZZLER -> getResourceAsText("/examplecode/swazzler.txt")
            CodeType.START_EMBRACE -> getResourceAsText("/examplecode/sdk.txt")
        }

        text = code?.replace("LAST_VERSION", embraceIntegrationDataProvider.getLastSDKVersion())

    }


    private fun getResourceAsText(path: String): String? =
        object {}.javaClass.getResource(path)?.readText()
}