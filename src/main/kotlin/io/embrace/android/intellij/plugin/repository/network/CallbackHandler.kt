package io.embrace.android.intellij.plugin.repository.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import io.embrace.android.intellij.plugin.ui.components.EmbEditableText
import org.apache.http.HttpStatus
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


internal class CallbackHandler(
    private val etAppId: EmbEditableText,
    private val etToken: EmbEditableText
    ) : HttpHandler {
    private val gson by lazy { Gson() }

    @Throws(IOException::class)
    override fun handle(exchange: HttpExchange) {
        val requestBody = readRequestBody(exchange.requestBody)
        updateUIElements(requestBody)
        writeResponseBody(exchange)
        exchange.close()
    }

    private fun readRequestBody(requestBody: InputStream): EmbraceCallbackRequestBody {
        val requestBodyString = requestBody.bufferedReader().use { it.readText() }
        requestBody.close()
        return gson.fromJson(requestBodyString, EmbraceCallbackRequestBody::class.java)
    }

    private fun updateUIElements(requestBody: EmbraceCallbackRequestBody) {
        etAppId.text = requestBody.appId
        etToken.text = requestBody.token
    }

    private fun writeResponseBody(exchange: HttpExchange) {
        val htmlBytes = this.javaClass.getResource(CALLBACK_RESPONSE_HTML_PATH)?.readBytes() ?: ByteArray(0)
        exchange.sendResponseHeaders(HttpStatus.SC_OK , htmlBytes.size.toLong())
        exchange.responseHeaders.set("Content-Type", "text/html")
        val os: OutputStream = exchange.responseBody
        os.write(htmlBytes)
        os.close()
    }
}

internal data class EmbraceCallbackRequestBody(
    @SerializedName("app_id")
    val appId: String,
    @SerializedName("token")
    val token: String
)

private const val CALLBACK_RESPONSE_HTML_PATH = "/html/callback_response.html"
