package io.embrace.android.intellij.plugin.data

internal enum class PluginType( val value : String) {
    V1("apply plugin: 'com.android.application'"),
    V2("id 'com.android.application'")
}