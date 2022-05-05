package com.tencent.devops.dispatch.bcs.client

import okhttp3.Headers
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class BcsClientCommon {

    companion object {
        private const val BCS_TOKEN_KEY = "BK-Devops-Token"
    }

    @Value("\${bcs.token}")
    val bcsToken: String = ""

    @Value("\${devopsGateway.idcProxy:#{null}}")
    val idcProxy: String? = null

    fun baseRequest(userId: String, url: String, headers: Map<String, String>? = null): Request.Builder {
        return Request.Builder().url(url(url)).headers(headers(headers))
    }

    fun url(realUrl: String) = "$idcProxy/proxy-devnet?url=${URLEncoder.encode(realUrl, "UTF-8")}"

    fun headers(otherHeaders: Map<String, String>? = null): Headers {
        val result = mutableMapOf<String, String>()

        val bcsHeaders = mapOf(BCS_TOKEN_KEY to bcsToken)
        result.putAll(bcsHeaders)

        if (!otherHeaders.isNullOrEmpty()) {
            result.putAll(otherHeaders)
        }

        return Headers.of(result)
    }
}
