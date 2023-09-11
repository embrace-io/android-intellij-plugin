package io.embrace.android.intellij.plugin.repository.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import io.embrace.android.intellij.plugin.data.EmbraceProject
import java.io.InputStream

private const val RESPONSE_SUCCESS = 204

internal abstract class BaseOnboardCallbackHandler (
    private val onSuccess: (EmbraceProject) -> Unit,
    private val onError: (String) -> Unit
) : HttpHandler {
    override fun handle(httpExchange: HttpExchange) {
        httpExchange.responseHeaders.add("Access-Control-Allow-Origin", "*")

        if (httpExchange.requestMethod.contains("OPTIONS")) {
            setOptionsHeaders(httpExchange)
            return
        }

        try {
            val requestBody = readRequestBody(httpExchange.requestBody)
            updateUIElements(requestBody)
            writeResponseBody(httpExchange, requestBody)
        } catch (e: InvalidRequestBodyException) {
            onError.invoke(e.message ?: "Invalid request body")
            writeErrorResponseBody(httpExchange)
        } finally {
            httpExchange.close()
        }
    }

    protected abstract fun readRequestBody(requestBody: InputStream): EmbraceCallbackRequestBody
    protected abstract fun writeResponseBody(httpExchange: HttpExchange, requestBody: EmbraceCallbackRequestBody)
    protected abstract fun writeErrorResponseBody(httpExchange: HttpExchange)

    /**
     * Ignores the first CORS endpoint and sets the necessary headers to allow the response.
     *
     * @param httpExchange The HttpExchange object representing the HTTP request and response.
     */
    private fun setOptionsHeaders(httpExchange: HttpExchange) {
        httpExchange.responseHeaders.add("Access-Control-Allow-Headers", "*")
        httpExchange.responseHeaders.add("Access-Control-Allow-Credentials", "true")
        httpExchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
        httpExchange.sendResponseHeaders(RESPONSE_SUCCESS, -1)
    }

    private fun updateUIElements(requestBody: EmbraceCallbackRequestBody) {
        if (requestBody.appId == null || requestBody.token == null) {
            onError.invoke("AppId or Token not found")
        } else {
            onSuccess.invoke(EmbraceProject(requestBody.appId, requestBody.token, requestBody.sessionId, requestBody.externalUserId))
        }
    }
}

internal class InvalidRequestBodyException(message: String) : Exception(message)