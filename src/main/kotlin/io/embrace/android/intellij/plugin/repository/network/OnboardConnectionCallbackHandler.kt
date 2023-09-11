package io.embrace.android.intellij.plugin.repository.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import io.embrace.android.intellij.plugin.data.EmbraceProject
import org.apache.http.HttpStatus
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles the response received from the browser through the localhost
 * for providing the app ID and token of a new project.
 */
internal class OnboardConnectionCallbackHandler(
    private val onSuccess: (EmbraceProject) -> Unit,
    private val onError: (String) -> Unit
) : HttpHandler {
    @Throws(IOException::class)
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

    private fun readRequestBody(requestBody: InputStream): EmbraceCallbackRequestBody {
        val requestBodyString = requestBody.bufferedReader().readText()
        requestBody.close()

        val pairs = requestBodyString.split("&")
        val paramMap = mutableMapOf<String, String>()

        pairs.forEach {
            val pair = it.split("=")
            paramMap[pair[0]] = pair[1]
        }

        val missingParams = REQUIRED_POST_KEYS.filter { !paramMap.containsKey(it) }
        if (missingParams.isNotEmpty()) {
            throw InvalidRequestBodyException("Missing required params: $missingParams")
        }

        return EmbraceCallbackRequestBody(
            paramMap["app_id"]!!,
            paramMap["token"]!!,
            paramMap["session_id"]!!,
            paramMap["external_user_id"]!!,
            paramMap["project_name"]!!
        )
    }

    private fun updateUIElements(requestBody: EmbraceCallbackRequestBody) {
        if (requestBody.appId == null || requestBody.token == null) {
            onError.invoke("AppId or Token not found")
        } else {
            onSuccess.invoke(EmbraceProject(requestBody.appId, requestBody.token, requestBody.sessionId, requestBody.externalUserId))
        }
    }

    private fun writeResponseBody(exchange: HttpExchange, requestBody: EmbraceCallbackRequestBody) {
        var template = String(this.javaClass.getResource(CALLBACK_RESPONSE_HTML_PATH)?.readBytes() ?: ByteArray(0))
        template = template.replace("{{dashboardURL}}", System.getenv("DASHBOARD_URL") ?: "https://dash.embrace.io")
        template = template.replace("{{projectName}}", requestBody.projectName)

        val response = template.toByteArray()

        exchange.sendResponseHeaders(HttpStatus.SC_OK, response.size.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(response)
        os.close()
    }

    private fun writeErrorResponseBody(exchange: HttpExchange) {
        val response = this.javaClass.getResource(ERROR_RESPONSE_HTML_PATH)?.readBytes() ?: ByteArray(0)

        exchange.sendResponseHeaders(HttpStatus.SC_OK, response.size.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(response)
        os.close()
    }
}

private data class EmbraceCallbackRequestBody(
    val appId: String?,
    val token: String?,
    val sessionId: String?,
    val externalUserId: String,
    val projectName: String,
)

private class InvalidRequestBodyException(message: String) : Exception(message)

private const val CALLBACK_RESPONSE_HTML_PATH = "/html/callback_response.html"
private const val ERROR_RESPONSE_HTML_PATH = "/html/error_response.html"
private const val RESPONSE_SUCCESS = 204
private val REQUIRED_POST_KEYS = listOf("app_id", "token", "session_id", "external_user_id", "project_name")