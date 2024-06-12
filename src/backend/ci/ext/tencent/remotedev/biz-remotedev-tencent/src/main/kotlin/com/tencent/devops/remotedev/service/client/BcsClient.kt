package com.tencent.devops.remotedev.service.client

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.pojo.bcs.BcsResp
import com.tencent.devops.remotedev.pojo.bcs.BcsTaskData
import com.tencent.devops.remotedev.pojo.bcs.ExpandDiskData
import com.tencent.devops.remotedev.pojo.bcs.ExpandDiskValidateResp
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BcsClient @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    @Value("\${bcsCloud.apiUrl}")
    val bcsCloudUrl: String = ""

    @Value("\${bcsCloud.token}")
    val bcsToken: String = ""

    @Value("\${apigw.appCode}")
    val appCode: String = ""

    @Value("\${apigw.appToken}")
    val appToken: String = ""

    fun expandDiskValidate(
        data: ExpandDiskData
    ): ExpandDiskValidateResp? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/expanddisk/validate"
        val body = JsonUtil.toJson(data, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return OkhttpUtils.doHttp(request).resolveResponse<BcsResp<ExpandDiskValidateResp>>().data
    }

    fun expandDisk(
        data: ExpandDiskData
    ): BcsTaskData? {
        val url = "$bcsCloudUrl/api/v1/remotedevenv/expanddisk"
        val body = JsonUtil.toJson(data, false)
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return OkhttpUtils.doHttp(request).resolveResponse<BcsResp<BcsTaskData>>().data
    }

    private inline fun <reified T> okhttp3.Response.resolveResponse(): T {
        this.use {
            val responseContent = this.body!!.string()
            logger.info("remotedev request bcs ${this.request.url} resp ${this.rid()}|$responseContent}")
            if (this.isSuccessful) {
                return objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            }

            val responseData = try {
                objectMapper.readValue<BcsResp<Void>>(responseContent)
            } catch (e: JacksonException) {
                throw RemoteServiceException(responseContent, this.code)
            }
            throw RemoteServiceException(responseData.message ?: responseData.code.toString(), this.code)
        }
    }

    private fun makeHeaders(): Map<String, String> {
        val headerMap = mapOf("bk_app_code" to appCode, "bk_app_secret" to appToken)
        val headerStr = objectMapper.writeValueAsString(headerMap).replace("\\s".toRegex(), "")
        return mapOf("X-Bkapi-Authorization" to headerStr, "BK-Devops-Token" to bcsToken)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BcsClient::class.java)
        private fun okhttp3.Response.rid(): String? {
            return this.headers["x-request-id"]
        }
    }
}