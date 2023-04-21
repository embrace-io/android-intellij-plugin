package io.embrace.android.intellij.plugin.dataproviders

import io.embrace.android.intellij.plugin.network.ApiClient
import org.json.JSONException
import org.json.JSONObject


class EmbraceIntegrationDataProvider(private val apiClient: ApiClient) {

    fun getLastSDKVersion(): String {
        val response = apiClient.sendGetRequest(EMBRACE_SDK_VERSION_URL)
        return try {
            val jsonObject = JSONObject(response)
            jsonObject.getString("value")
        } catch (e: JSONException) {
            println("An error occurred on json parser for getting last version")
            e.printStackTrace()
            ""
        }
    }

    companion object {
        const val EMBRACE_SDK_VERSION_URL: String = "https://dash-api.embrace.io/external/sdk/android/version"
    }
}
