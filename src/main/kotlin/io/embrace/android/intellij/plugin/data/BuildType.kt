package io.embrace.android.intellij.plugin.data


internal enum class BuildType(val application: String, val swazzler: String) {
    GROOVY_V1("apply plugin: 'com.android.application'", "apply plugin: 'embrace-swazzler'"),
    GROOVY_V2("id 'com.android.application'", "id 'embrace-swazzler'"),
    KOTLIN_DSL("id ('com.android.application')", "id(\"embrace-swazzler\")")
}




