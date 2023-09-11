package io.embrace.android.intellij.plugin.repository.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.sun.net.httpserver.HttpExchange
import io.embrace.android.intellij.plugin.data.EmbraceProject
import org.apache.http.HttpStatus
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles the response received from the browser through the localhost
 * for providing the app ID and token of a new project.
 *
 * This callback is expecting the data coming from a JSON post
 */
private data class EmbraceCallbackResponseBody(
    @SerializedName("success")
    val success: Boolean = true
)

internal class OnboardConnectionCallbackHandler(
    onSuccess: (EmbraceProject) -> Unit,
    onError: (String) -> Unit
) : BaseOnboardCallbackHandler(onSuccess, onError) {

    private val gson by lazy { Gson() }

    override fun readRequestBody(requestBody: InputStream): EmbraceCallbackRequestBody {
        val requestBodyString = requestBody.bufferedReader().use { it.readText() }
        requestBody.close()

        try {
            return gson.fromJson(requestBodyString, EmbraceCallbackRequestBody::class.java)
        } catch (e: Exception) {
            throw InvalidRequestBodyException("Invalid request body")
        }
    }

    override fun writeResponseBody(httpExchange: HttpExchange, requestBody: EmbraceCallbackRequestBody) {
        val response = gson.toJson(EmbraceCallbackResponseBody()).toByteArray()

        httpExchange.sendResponseHeaders(HttpStatus.SC_OK, response.size.toLong())
        val os: OutputStream = httpExchange.responseBody
        os.write(response)
        os.close()
    }

    override fun writeErrorResponseBody(httpExchange: HttpExchange) {
        val response = gson.toJson(EmbraceCallbackResponseBody(success = false)).toByteArray()

        httpExchange.sendResponseHeaders(HttpStatus.SC_OK, response.size.toLong())
        val os: OutputStream = httpExchange.responseBody
        os.write(response)
        os.close()
    }
}