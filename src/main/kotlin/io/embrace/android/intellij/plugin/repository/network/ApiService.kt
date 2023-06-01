package io.embrace.android.intellij.plugin.repository.network

import org.json.JSONException
import org.json.JSONObject

internal class ApiService {
    private val apiClient = ApiClient()

    companion object {
        const val EMBRACE_SDK_VERSION_URL: String = "https://dash-api.embrace.io/external/sdk/android/version"
        const val EMBRACE_DASHBOARD_URL: String = "https://dash.stg.emb-eng.com/onboard/project"
        const val EMBRACE_DASHBOARD_COMPLETE_INTEGRATION: String = "https://dash.stg.emb-eng.com/onboard/build-and-run/{appId}"
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
}