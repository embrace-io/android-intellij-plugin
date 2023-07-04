package io.embrace.android.intellij.plugin.dataproviders.callback

internal interface VerifyIntegrationCallback {
    fun onEmbraceIntegrationSuccess()
    fun onEmbraceIntegrationError()
}