package com.tencent.devops.remotedev.service.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.remotedev.config.RemoteDevCommonConfig
import java.io.IOException
import com.tencent.devops.remotedev.pojo.startcloud.StartCloudAppCreateReq
import com.tencent.devops.remotedev.pojo.startcloud.StartCloudAppCreateRespData
import com.tencent.devops.remotedev.pojo.startcloud.StartCloudComputerStatusReqBody
import com.tencent.devops.remotedev.pojo.startcloud.StartCloudComputerStatusRespData
import com.tencent.devops.remotedev.pojo.startcloud.StartCloudNoDataResp
import com.tencent.devops.remotedev.pojo.startcloud.StartCloudResp
import com.tencent.devops.remotedev.pojo.startcloud.StartMessageRegisterReq
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StartCloudClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val config: RemoteDevCommonConfig
) {

    fun computerStatus(
        cgsIds: Set<String>?
    ): List<StartCloudComputerStatusRespData>? {
        val url = "${config.apiUrl}/openapi/computer/status"
        val body = JsonUtil.toJson(
            StartCloudComputerStatusReqBody(
                appName = config.bkciAppName,
                cgsIds = cgsIds
            ),
            false
        )
        logger.debug("computerStatus request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(genStartApiHeaders(body).toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = doRequest(request).resolveResponse<StartCloudResp<List<StartCloudComputerStatusRespData>>>()
        if (resp.code != 0) {
            logger.warn("request /computer/status error ${resp.code}|${resp.message}")
            throw RemoteServiceException(
                errorMessage = "request api[${request.url.toUrl()}] error ${resp.message}",
                errorCode = resp.code
            )
        }

        return resp.data
    }

    fun appCreate(
        appName: String,
        detail: String
    ): Long? {
        val url = "${config.apiUrl}/openapi/app/create"
        val body = JsonUtil.toJson(
            StartCloudAppCreateReq(
                contentProviderName = config.contentProviderName,
                appName = appName,
                detail = detail
            ),
            false
        )
        logger.debug("$appName $detail request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(genStartApiHeaders(body).toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = doRequest(request).resolveResponse<StartCloudResp<StartCloudAppCreateRespData>>()
        if (resp.code != 0) {
            logger.warn("request /app/create error ${resp.code}|${resp.message}")
            throw RemoteServiceException(
                errorMessage = "request api[${request.url.toUrl()}] error ${resp.message}",
                errorCode = resp.code
            )
        }

        return resp.data?.appId
    }

    fun messageRegister(
        req: StartMessageRegisterReq
    ) {
        val url = "${config.apiUrl}/openapi/push/message/register"
        val body = JsonUtil.toJson(req, false)
        logger.debug("messageRegister request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(genStartApiHeaders(body).toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = doRequest(request).resolveResponse<StartCloudNoDataResp>()
        if (resp.code != 0) {
            logger.warn("request /message/register error ${resp.code}|${resp.message}")
            throw RemoteServiceException(
                errorMessage = "request api[${request.url.toUrl()}] error ${resp.message}",
                errorCode = resp.code
            )
        }
    }

    private fun genStartApiHeaders(
        body: String
    ): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["x-start-appid"] = config.appId
        val timestampMillis = System.currentTimeMillis().toString().take(10)
        headerBuilder["x-start-timestamp"] = timestampMillis
        headerBuilder["x-start-signature"] =
            ShaUtils.sha256("${config.appId}${config.appKey}$timestampMillis$body").uppercase()

        return headerBuilder
    }

    private fun doRequest(request: Request): okhttp3.Response {
        try {
            return OkhttpUtils.doHttp(request)
        } catch (e: IOException) {
            throw RemoteServiceException("request api[${request.url.toUrl()}] error: ${e.localizedMessage}")
        }
    }

    private inline fun <reified T> okhttp3.Response.resolveResponse(): T {
        this.use {
            val responseContent = this.body!!.string()
            if (!this.isSuccessful) {
                throw RemoteServiceException("request api[${this.request.url.toUrl()}] error", this.code)
            }

            val responseData = try {
                objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            } catch (e: Exception) {
                throw RemoteServiceException("parse api[${this.request.url.toUrl()}] resp $responseContent", this.code)
            }

            return responseData
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartCloudClient::class.java)
    }
}
