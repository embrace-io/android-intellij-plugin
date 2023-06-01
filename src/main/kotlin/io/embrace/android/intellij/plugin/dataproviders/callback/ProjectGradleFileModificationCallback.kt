package io.embrace.android.intellij.plugin.dataproviders.callback

internal interface ProjectGradleFileModificationCallback {
    fun onGradleFileError(error: String)
    fun onGradleFileAlreadyModified()
    fun onGradleFilesModifiedSuccessfully()
}