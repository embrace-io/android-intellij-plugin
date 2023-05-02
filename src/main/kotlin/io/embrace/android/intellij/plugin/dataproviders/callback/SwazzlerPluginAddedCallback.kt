package io.embrace.android.intellij.plugin.dataproviders.callback

internal interface SwazzlerPluginAddedCallback {
    fun onSwazzlerPluginAdded()
    fun onSwazzlerPluginError(error: String)
}