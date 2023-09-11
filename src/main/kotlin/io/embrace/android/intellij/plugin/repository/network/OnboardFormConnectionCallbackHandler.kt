package io.embrace.android.intellij.plugin.repository.network

import com.sun.net.httpserver.HttpExchange
import io.embrace.android.intellij.plugin.data.EmbraceProject
import org.apache.http.HttpStatus
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles the response received from the browser through the localhost
 * for providing the app ID and token of a new project.
 *
 * This callback is expecting the data coming from a form submission.
 */
internal class OnboardFormConnectionCallbackHandler(
    onSuccess: (EmbraceProject) -> Unit,
    onError: (String) -> Unit
) : BaseOnboardCallbackHandler(onSuccess, onError) {
    @Throws(IOException::class)
    override fun readRequestBody(requestBody: InputStream): EmbraceCallbackRequestBody {
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

    override fun writeResponseBody(httpExchange: HttpExchange, requestBody: EmbraceCallbackRequestBody) {
        var template = String(this.javaClass.getResource(CALLBACK_RESPONSE_HTML_PATH)?.readBytes() ?: ByteArray(0))
        template = template.replace("{{dashboardURL}}", ApiService.DASHBOARD_URL)
        template = template.replace("{{projectName}}", requestBody.projectName)

        val response = template.toByteArray()

        httpExchange.sendResponseHeaders(HttpStatus.SC_OK, response.size.toLong())
        val os: OutputStream = httpExchange.responseBody
        os.write(response)
        os.close()
    }

    override fun writeErrorResponseBody(httpExchange: HttpExchange) {
        val response = this.javaClass.getResource(ERROR_RESPONSE_HTML_PATH)?.readBytes() ?: ByteArray(0)

        httpExchange.sendResponseHeaders(HttpStatus.SC_OK, response.size.toLong())
        val os: OutputStream = httpExchange.responseBody
        os.write(response)
        os.close()
    }
}

private const val CALLBACK_RESPONSE_HTML_PATH = "/html/callback_response.html"
private const val ERROR_RESPONSE_HTML_PATH = "/html/error_response.html"
private val REQUIRED_POST_KEYS = listOf("app_id", "token", "session_id", "external_user_id", "project_name")