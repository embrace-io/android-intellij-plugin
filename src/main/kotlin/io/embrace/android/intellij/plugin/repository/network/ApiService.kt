package io.embrace.android.intellij.plugin.repository.network

import io.embrace.android.intellij.plugin.data.EmbraceProject
import io.embrace.android.intellij.plugin.dataproviders.callback.VerifyIntegrationCallback
import org.json.JSONException
import org.json.JSONObject

internal class ApiService {
    private val apiClient = ApiClient()

    companion object {
        const val EMBRACE_SDK_VERSION_URL: String = "https://dash-api.embrace.io/external/sdk/android/version"

        const val EMBRACE_CREATE_PROJECT_URL: String =
            "https://dash.android-plugin.joaquin-diaz.dev.emb-eng.com/onboard/android-plugin"

        const val EMBRACE_DASHBOARD_VERIFY_INTEGRATION_URL: String =
            "https://dash-api.android-plugin.joaquin-diaz.dev.emb-eng.com/external/v4/org/app/{appId}/verify_integration"

        const val EMBRACE_DASHBOARD_URL: String =
            "https://dash.embrace.io/app/{appId}/grouped_sessions/hour"
    }

    fun getLastSDKVersion(): String {
        val response = apiClient.executeGetRequest(EMBRACE_SDK_VERSION_URL)
        return try {
            val jsonObject = JSONObject(response)
            jsonObject.getString("value")
        } catch (e: JSONException) {
            println("An error occurred on json parser for getting last version")
            e.printStackTrace()
            ""
        }
    }

    fun verifyIntegration(embraceProject: EmbraceProject, callback: VerifyIntegrationCallback) {
        val url = EMBRACE_DASHBOARD_VERIFY_INTEGRATION_URL.replace("{appId}", embraceProject.appId)

        apiClient.executeGetRequestAsync(url, embraceProject.sessionId) { response ->
            if (response.contains("\"integration_verified\":true")) {
                callback.onEmbraceIntegrationSuccess()
            } else {
                callback.onEmbraceIntegrationError()
            }
        }
    }
}