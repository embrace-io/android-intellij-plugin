package io.embrace.android.intellij.plugin.repository.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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

    private val gson by lazy { Gson() }

    @Throws(IOException::class)
    override fun handle(httpExchange: HttpExchange) {
        httpExchange.responseHeaders.add("Access-Control-Allow-Origin", "*")

        if (httpExchange.requestMethod.contains("OPTIONS")) {
            setOptionsHeaders(httpExchange)
            return
        }

        val requestBody = readRequestBody(httpExchange.requestBody)
        updateUIElements(requestBody)
        writeResponseBody(httpExchange)

        httpExchange.close()
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
        val requestBodyString = requestBody.bufferedReader().use { it.readText() }
        requestBody.close()
        return gson.fromJson(requestBodyString, EmbraceCallbackRequestBody::class.java)
    }

    private fun updateUIElements(requestBody: EmbraceCallbackRequestBody) {
        if (requestBody.appId == null || requestBody.token == null) {
            onError.invoke("AppId or Token not found")
        } else {
            onSuccess.invoke(EmbraceProject(requestBody.appId, requestBody.token, requestBody.sessionId))
        }
    }

    private fun writeResponseBody(exchange: HttpExchange) {
        val htmlBytes = this.javaClass.getResource(CALLBACK_RESPONSE_HTML_PATH)?.readBytes() ?: ByteArray(0)
        exchange.sendResponseHeaders(HttpStatus.SC_OK, htmlBytes.size.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(htmlBytes)
        os.close()
    }
}

private data class EmbraceCallbackRequestBody(
    @SerializedName("app_id")
    val appId: String?,

    @SerializedName("token")
    val token: String?,

    @SerializedName("sessionid")
    val sessionId: String?
)

private const val CALLBACK_RESPONSE_HTML_PATH = "/html/callback_response.html"
private const val RESPONSE_SUCCESS = 204