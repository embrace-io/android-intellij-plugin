package io.embrace.android.intellij.plugin.dataproviders.callback

internal interface ConfigFileCreationCallback {
    fun onConfigSuccess()
    fun onConfigAlreadyExists()
    fun onConfigError(error : String)
}