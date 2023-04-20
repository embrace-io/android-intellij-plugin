package io.embrace.android.intellij.plugin.ui.components

import java.awt.Color
import java.awt.Font
import javax.swing.JLabel

internal class EmbBlockCode(block: CODE_BLOCK) : JLabel() {

    enum class CODE_BLOCK {
        SWAZZLER,
        SDK,
        START_EMBRACE
    }

    init {
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        isOpaque = true
        background = Color.decode("#5c5c5c") // dark gray

        text = when (block) {
            CODE_BLOCK.SDK -> """<html><pre><code>buildscript {
                    repositories {
                            mavenCentral()
                            google()
                    }

                    dependencies {
                        classpath 'io.embrace:embrace-swazzler:5.14.2'
                    }
                    }</code></pre></html>"""

            CODE_BLOCK.SWAZZLER -> """<html><pre><code>buildscript {
                    repositories {
                            mavenCentral()
                            google()
                    }

                    dependencies {
                            classpath 'io.embrace:embrace-swazzler:5.14.2'
                    }
                    }</code></pre></html>"""

            CODE_BLOCK.START_EMBRACE -> "<html><pre><code>" +
                    "import io.embrace.android.embracesdk.Embrace\n" +
                    "\n" +
                    "class MyApplication : Application() {\n" +
                    "    override fun onCreate() {\n" +
                    "        super.onCreate()\n" +
                    "        Embrace.getInstance().start(this)\n" +
                    "        EmbraceSamples.verifyIntegration() // temporarily add this to verify the integration\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre></html>"
        }
    }
}