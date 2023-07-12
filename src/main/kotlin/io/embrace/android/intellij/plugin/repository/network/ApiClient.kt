package io.embrace.android.intellij.plugin.repository.network

import io.sentry.Sentry
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ApiClient {
    private val httpClient: HttpClient = HttpClient.newBuilder().build()

    fun executeGetRequest(url: String): String? {
        var response: String? = null

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI(url))
            .GET()
            .build()

        try {
            val httpResponse: HttpResponse<String> = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            response = httpResponse.body()
        } catch (e: IOException) {
            Sentry.captureException(e)
            println("Error sending GET request")
        } catch (e: InterruptedException) {
            Sentry.captureException(e)
            println("Error sending GET request")
        }

        return response
    }

    fun executeGetRequestAsync(url: String, sessionId : String?, callback: (String) -> Unit) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("SessionId", sessionId)
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                val responseBody = response.body()
                println(responseBody)
                callback.invoke(responseBody)
            }
    }


}
