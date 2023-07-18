package io.embrace.android.intellij.plugin.repository.sentry

import io.embrace.android.intellij.plugin.ui.components.IntegrationStep

internal interface SentryLogger {

    fun logException(e: Exception)
    fun logMessage(message: String)
    fun addAppIdTag(appId: String)
    fun logStepCompleted(step: IntegrationStep)
}