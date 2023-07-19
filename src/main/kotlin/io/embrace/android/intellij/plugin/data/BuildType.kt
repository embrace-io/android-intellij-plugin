package io.embrace.android.intellij.plugin.data


internal enum class BuildType {
    APPLY_PLUGIN, // apply plugin
    PLUGIN_ID_GROOVY,  //  plugins { id 'my id' }
    PLUGIN_ID_KOTLIN,// "id(\"embrace-swazzler\")"
    NOT_IDENTIFY // not identify
}




