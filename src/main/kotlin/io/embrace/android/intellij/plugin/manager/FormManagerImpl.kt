package io.embrace.android.intellij.plugin.manager

import io.embrace.android.intellij.plugin.network.NetworkManager
import org.json.JSONException
import org.json.JSONObject


class FormManagerImpl(private val networkManager: NetworkManager) : FormManager {
    override fun getLastSDKVersion(): String {
        val response = networkManager.sendGetRequest(URL)
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
        const val URL: String = "https://dash-api.embrace.io/external/sdk/android/version"
    }
}
