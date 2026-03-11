package com.tencent.devops.project.service.taihu

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.project.pojo.taihu.TaiUserInfo
import com.tencent.devops.project.pojo.taihu.TaiUserInfoRequest
import com.tencent.devops.project.pojo.taihu.TaiUserInfoResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TaiHuService @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TaiHuService::class.java)
    }

    @Value("\${taiHu.apiUrl}")
    val apiUrl: String = ""

    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    fun getTaiUserInfo(params: TaiUserInfoRequest): List<TaiUserInfo> {
        if (params.usernames.isEmpty()) return emptyList()
        val authorization = """{"bk_app_code":"$appCode","bk_app_secret":"$appSecret"}"""
        val requestBody = JsonUtil.toJson(bean = params, formatted = false)
        val request = Request.Builder()
            .url("$apiUrl/prod/api/v1/open/odc-tai/users/-/query/")
            .header("X-Bkapi-Authorization", authorization)
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        val res = OkhttpUtils.doHttp(request).resolveResponse<TaiUserInfoResponse>()
        return res.data
    }

    private inline fun <reified T> okhttp3.Response.resolveResponse(): T {
        this.use {
            val responseContent = this.body!!.string()
            if (!this.isSuccessful) {
                throw RemoteServiceException(
                    "request api[${this.request.url.toUrl()}] error|$responseContent",
                    this.code
                )
            }

            val responseData = try {
                objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            } catch (ignored: Exception) {
                logger.error("TaiClient resolveResponse fail|${ignored.message}", ignored)
                throw RemoteServiceException("parse api[${this.request.url.toUrl()}] resp $responseContent", this.code)
            }

            return responseData
        }
    }
}
