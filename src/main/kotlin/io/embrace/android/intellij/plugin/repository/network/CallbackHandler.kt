package io.embrace.android.intellij.plugin.repository.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import org.apache.http.HttpStatus
import org.gradle.internal.resource.transport.http.HttpResourceAccessor
import org.gradle.internal.resource.transport.http.HttpResponseResource
import java.io.IOException
import java.io.OutputStream


internal class CallbackHandler : HttpHandler {
    @Throws(IOException::class)
    override fun handle(exchange: HttpExchange) {
        val response = "Received callback!"
        exchange.sendResponseHeaders(HttpStatus.SC_OK , response.length.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(response.toByteArray())
        os.close()
    }
}