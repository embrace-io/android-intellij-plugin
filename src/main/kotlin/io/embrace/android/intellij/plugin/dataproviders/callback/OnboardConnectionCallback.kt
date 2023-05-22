package io.embrace.android.intellij.plugin.dataproviders.callback

internal interface OnboardConnectionCallback {
    fun onOnboardConnected(appId: String, token: String)
    fun onOnboardConnectedError(error: String)
}