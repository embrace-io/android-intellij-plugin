package io.embrace.android.intellij.plugin.network

interface NetworkManager {
    fun sendGetRequest(url: String): String?
}
