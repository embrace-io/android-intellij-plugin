package io.embrace.android.intellij.plugin.repository.network

import io.embrace.android.intellij.plugin.data.EmbraceProject
import io.embrace.android.intellij.plugin.repository.sentry.SentryLogger
import org.json.JSONException
import org.json.JSONObject

internal class ApiService {
    private val apiClient = ApiClient()

    companion object {
        val DASHBOARD_URL = System.getenv("DASHBOARD_URL") ?: "https://dash.embrace.io"

        val EMBRACE_CREATE_PROJECT_URL: String = "${DASHBOARD_URL}/android-plugin/landing"

        val EMBRACE_DASHBOARD_URL: String =
                "${DASHBOARD_URL}/app/{appId}/grouped_sessions/hour?android_plugin_integration=success"

        const val EMBRACE_SDK_VERSION_URL: String = "https://dash-api.embrace.io/external/sdk/android/version"

        const val EMBRACE_DASHBOARD_VERIFY_INTEGRATION_URL: String =
            "https://dash-api.embrace.io/external/v4/org/app/{appId}/verify_integration"
    }

    fun getLastSDKVersion(): String {
        val response = apiClient.executeGetRequest(EMBRACE_SDK_VERSION_URL)
        return try {
            val jsonObject = JSONObject(response)
            jsonObject.getString("value")
        } catch (e: JSONException) {
            println("An error occurred on json parser for getting last version")
            SentryLogger.logException(e)
            ""
        }
    }

    fun verifyIntegration(embraceProject: EmbraceProject, onSuccess: () -> Unit, onError: () -> Unit) {
        val url = EMBRACE_DASHBOARD_VERIFY_INTEGRATION_URL.replace("{appId}", embraceProject.appId)

        apiClient.executeGetRequestAsync(url, embraceProject.sessionId) { response ->
            if (response.contains("\"integration_verified\":true")) {
                onSuccess.invoke()
            } else {
                onError.invoke()
            }
        }
    }
}