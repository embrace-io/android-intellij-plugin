package io.embrace.android.intellij.plugin.repository.sentry

import com.intellij.application.options.colors.pluginExport.ColorSchemePluginTemplate
import com.intellij.openapi.project.Project
import io.embrace.android.intellij.plugin.ui.components.IntegrationStep
import io.sentry.Sentry

private const val SENTRY_DNS = "https://816ef499adf04f0c8ce55ff0a879974a@s.embrace.io/96"
private const val PLUGIN_VERSION = "1.0.0"
private const val FLUSH_TIMEOUT_MS = 5000L


private const val SENTRY_TAG_APP_ID = "app-id"
private const val SENTRY_STEP_COMPLETE = "step-complete"


internal class DefaultSentryLogger(project: Project, isEnable: Boolean) : SentryLogger {

    init {
        if (isEnable) {
            Sentry.init { options ->
                options.dsn = SENTRY_DNS
                options.flushTimeoutMillis = FLUSH_TIMEOUT_MS
                options.release = ColorSchemePluginTemplate.PLUGIN_VERSION
                options.tracesSampleRate = 1.0
            }
        }
    }

    /**
     * Given a key-value pair, creates a new tag and associates it with the Sentry instance.
     *
     *
     * If the key is null or the result the key's `toString()` method is blank, no attempt is done
     * to add the tag and this method is effectively a no-op.
     */
    @Synchronized
    private fun addTag(key: String, value: Any?) {
        if (key.isNotBlank()) {
            Sentry.setTag(key, value.toString())
        }
    }

    override fun logException(e: Exception) {
        Sentry.captureException(e)
    }

    override fun logMessage(message: String) {
        Sentry.captureMessage(message)
    }


    override fun addAppIdTag(appId: String) {
        addTag(SENTRY_TAG_APP_ID, appId)
    }

    override fun logStepCompleted(step: IntegrationStep) {
        addTag(SENTRY_STEP_COMPLETE, step.name)
    }


}