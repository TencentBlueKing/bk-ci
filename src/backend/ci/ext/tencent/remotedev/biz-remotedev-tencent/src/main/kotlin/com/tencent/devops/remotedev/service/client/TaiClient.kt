package com.tencent.devops.remotedev.service.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.config.BkConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TaiClient @Autowired constructor(
    private val bkConfig: BkConfig,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TaiClient::class.java)
    }

    @Value("\${taiHu.apiUrl}")
    val apiUrl: String = ""

    fun taiUserInfo(params: TaiUserInfoRequest): List<TaiUserInfo> {
        if (params.usernames.isEmpty()) return emptyList()
        val authorization = """{"bk_app_code":"${bkConfig.appCode}","bk_app_secret":"${bkConfig.appSecret}"}"""
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
            } catch (e: Exception) {
                logger.error("TaiClient resolveResponse fail|${e.message}", e)
                throw RemoteServiceException("parse api[${this.request.url.toUrl()}] resp $responseContent", this.code)
            }

            return responseData
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaiUserInfoRequest(
    val usernames: Set<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaiUserInfoResponse(
    val data: List<TaiUserInfo>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaiUserInfo(
    val username: String,
    @JsonProperty("account_name")
    val accountName: String,
    @JsonProperty("account_email")
    val accountEmail: String,
    @JsonProperty("company_tags")
    val companyTags: List<CompanyTags>,
    val departments: List<DepartmentsInfo>?
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CompanyTags(
        @JsonProperty("tag_id")
        val tagId: String,
        @JsonProperty("tag_name")
        val tagName: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DepartmentsInfo(
        val id: Long,
        val name: String
    )
}
