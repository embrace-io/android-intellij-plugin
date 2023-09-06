package io.embrace.android.intellij.plugin.services

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.emptyJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class TrackingEvent(val humanReadableName: String) {
    DASHBOARD_CONNECTED("Dashboard Connected"),
    DASHBOARD_CONNECTION_FAILED("Dashboard Connection Failed"),
    CONFIGURATION_FILE_CREATED("Configuration File Created"),
    CONFIGURATION_FILE_CREATION_FAILED("Configuration File Creation Failed"),
    GRADLE_FILE_MODIFIED("Gradle File Modified"),
    GRADLE_FILE_ALREADY_MODIFIED("Gradle File Already Modified"),
    GRADLE_FILE_MODIFICATION_FAILED("Gradle File Modification Failed"),
    START_SDK_ADDED("Start SDK Added"),
    START_SDK_ALREADY_ADDED("Start SDK Already Added"),
    START_SDK_ADDITION_FAILED("Start SDK Addition Failed"),
    INTEGRATION_SUCCEEDED("Integration Succeeded"),
    INTEGRATION_FAILED("Integration Failed"),
    STEP_SKIPPED("Step Skipped"),
    OPEN_DASHBOARD_FROM_PLUGIN("Open Dashboard From Plugin"),
    OPEN_DASHBOARD_FROM_PLUGIN_FAILED("Open Dashboard From Plugin Failed"),
}

const val pluginPackage = "io.embrace.android.intellij.plugin"

@Service
class TrackingService {
    // TODO: figure out a way of disabling segment on local
    private val segmentWriteKey = System.getenv("SEGMENT_WRITE_KEY") ?: "7kDcjON7hJhoLrThnta9I54VFL07vIvm"
    private val enabled = true
    private lateinit var analytics: Analytics
    private val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId(pluginPackage))?.version
    private var appId: String = ""

    init {
        if (enabled) {
            analytics = Analytics(segmentWriteKey) {
                application = "EmbraceAndroidPlugin"
                flushAt = 3
                flushInterval = 10
            }
        }
    }

    fun identify(userId: String, appId: String) {
        if (!enabled) return

        analytics.identify(userId)

        this.appId = appId
    }

    fun trackEvent(eventName: TrackingEvent, properties: JsonObject? = emptyJsonObject) {
        if (!enabled) return

        val extendedProperties = buildJsonObject {
            put("plugin_version", pluginVersion ?: "unknown")
            put("is_android_plugin", true)
            put("app_id", appId)

            properties?.forEach {
                put(it.key, it.value)
            }
        }

        analytics.track(eventName.humanReadableName, extendedProperties)
    }
}