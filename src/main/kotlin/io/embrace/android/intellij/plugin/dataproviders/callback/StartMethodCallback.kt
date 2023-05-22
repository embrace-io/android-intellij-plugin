package io.embrace.android.intellij.plugin.dataproviders.callback

import io.embrace.android.intellij.plugin.dataproviders.StartMethodStatus

internal interface StartMethodCallback {
    fun onStartStatusUpdated(status: StartMethodStatus)
}