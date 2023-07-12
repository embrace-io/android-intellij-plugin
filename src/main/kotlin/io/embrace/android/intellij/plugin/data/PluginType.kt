package io.embrace.android.intellij.plugin.data


internal enum class PluginType( val application: String,  val swazzler: String) {
    V1("apply plugin: 'com.android.application'", "apply plugin: 'embrace-swazzler'"),
    V2("id 'com.android.application'", "id 'embrace-swazzler'"),
    V3("id ('com.android.application')", "id(\"embrace-swazzler\")")
}
