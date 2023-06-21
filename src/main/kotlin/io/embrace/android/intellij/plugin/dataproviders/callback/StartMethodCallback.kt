package io.embrace.android.intellij.plugin.dataproviders.callback

import io.embrace.android.intellij.plugin.data.StartMethodStatus

internal interface StartMethodCallback {
    fun onStartStatusUpdated(status: StartMethodStatus)
}