package io.embrace.android.intellij.plugin.dataproviders.callback

internal interface StartMethodCallback {
    fun onStartAdded()
    fun onStartError(error : String)
}