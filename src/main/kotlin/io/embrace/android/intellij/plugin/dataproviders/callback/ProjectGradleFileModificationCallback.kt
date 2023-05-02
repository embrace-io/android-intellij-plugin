package io.embrace.android.intellij.plugin.dataproviders.callback

internal interface ProjectGradleFileModificationCallback {
    fun onGradleContentModified()
    fun onGradleFileError(error: String)
    fun onGradleContentFound(newLine: String, contentToModify: String)
}