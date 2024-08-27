package com.tencent.devops.remotedev.service.client

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException

@Suppress("ALL")
@Component
class RemotedevBkRepoClient @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    @Value("\${bkrepo.bkrepoDevxHeaderUserAuth:#{null}}")
    val bkrepoDevxHeaderUserAuth: String? = null

    @Value("\${bkrepo.bkrepoMediaUrl:}")
    val bkrepoMediaUrl = ""

    @Value("\${bkrepo.bkrepoMediaHeaderUserAuth:}")
    val bkrepoMediaHeaderUserAuth = ""

    fun repoStreamCreate(
        projectId: String,
        workspaceName: String,
        userId: String
    ): String? {
        val request = Request.Builder()
            .url("$bkrepoMediaUrl/media/api/user/stream/create/$projectId/$workspaceName?display=false")
            .headers(getCommonHeaders(userId, true).toHeaders())
            .post(
                objectMapper.writeValueAsString(JsonUtil.toJson(mapOf<String, String>()))
                    .toRequestBody(MediaTypes.APPLICATION_JSON.toMediaTypeOrNull())
            )
            .build()
        return doRequest(request).resolveResponse<Response<String>>()?.data
    }

    private fun getCommonHeaders(userId: String, isMedia: Boolean = false): MutableMap<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = if (!isMedia) {
            bkrepoDevxHeaderUserAuth ?: ""
        } else {
            bkrepoMediaHeaderUserAuth
        }
        headers["X-BKREPO-UID"] = userId
        return headers
    }

    private fun doRequest(request: Request): okhttp3.Response {
        try {
            return OkhttpUtils.doHttp(request)
        } catch (e: IOException) {
            throw RemoteServiceException("request api[${request.url.toUrl()}] error: ${e.localizedMessage}")
        }
    }

    private inline fun <reified T> okhttp3.Response.resolveResponse(allowCode: Int? = null): T? {
        this.use {
            val responseContent = this.body!!.string()
            logger.debug("remotedev request bkrepo {} resp {}", this.request.url, responseContent)
            if (this.isSuccessful) {
                return objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            }

            val responseData = try {
                objectMapper.readValue<Response<Void>>(responseContent)
            } catch (e: JacksonException) {
                throw RemoteServiceException(responseContent, this.code)
            }
            if (allowCode == responseData.code) {
                logger.info("request bkrepo api failed but it can be allowed: ${responseData.message}")
                return null
            }
            throw RemoteServiceException(responseData.message ?: responseData.code.toString(), this.code)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoClient::class.java)
    }
}
