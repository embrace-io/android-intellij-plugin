package io.embrace.android.intellij.plugin.network

interface ApiClient {
    fun sendGetRequest(url: String): String?
}
