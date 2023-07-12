package io.embrace.android.intellij.plugin.repository

import com.intellij.application.options.colors.pluginExport.ColorSchemePluginTemplate.PLUGIN_VERSION
import io.sentry.Sentry

private const val SENTRY_DNS = "https://816ef499adf04f0c8ce55ff0a879974a@s.embrace.io/96"
private const val PLUGIN_VERSION = "1.0.0"


internal class DefaultSentryLogger(isEnable: Boolean) {

    init {
        if (isEnable) {
            Sentry.init { options ->
                options.dsn = SENTRY_DNS
                options.release = PLUGIN_VERSION
                options.tracesSampleRate = 1.0
            }
        }
    }
}