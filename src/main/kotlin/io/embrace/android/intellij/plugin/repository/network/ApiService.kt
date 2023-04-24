package io.embrace.android.intellij.plugin.repository.network

import org.json.JSONException
import org.json.JSONObject

class ApiService {
    private val apiClient = ApiClient()

    companion object {
        const val EMBRACE_SDK_VERSION_URL: String = "https://dash-api.embrace.io/external/sdk/android/version"
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