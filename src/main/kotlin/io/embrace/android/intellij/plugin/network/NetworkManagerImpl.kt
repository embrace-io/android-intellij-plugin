package io.embrace.android.intellij.plugin.network

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class NetworkManagerImpl : NetworkManager {

    private val httpClient: HttpClient = HttpClient.newBuilder().build()

    override fun sendGetRequest(url: String): String? {
        var response: String? = null

        val httpRequest: HttpRequest
        try {
            httpRequest = HttpRequest.newBuilder()
                .uri(URI(url))
                .GET()
                .build()
        } catch (e: URISyntaxException) {
            println("Error creating URI for URL: $url")
            return null
        }

        try {
            val httpResponse: HttpResponse<String> = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            response = httpResponse.body()
        } catch (e: IOException) {
            println("Error sending GET request")
        } catch (e: InterruptedException) {
            println("Error sending GET request")
        }

        return response
    }
}
