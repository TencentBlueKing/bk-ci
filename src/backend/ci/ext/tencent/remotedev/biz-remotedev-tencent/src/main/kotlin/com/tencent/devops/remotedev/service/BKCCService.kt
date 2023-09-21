package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 封装 bkcc 相关接口
 */
@Service
class BKCCService @Autowired constructor(
    private val objectMapper: ObjectMapper
) {

    @Value("\${bkCC.host:}")
    val ccHost: String = ""

    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    // 更新主机属性
    fun updateHost(
        hostIds: Set<String>,
        props: Map<String, Any>
    ) {
        val url = "$ccHost/update_host/"
        val body = UpdateHostReqBody(
            bkAppCode = appCode,
            bkAppSecret = appSecret,
            bkHostId = hostIds.joinToString { "," },
            data = props
        )
        val request = Request.Builder()
            .url(url)
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                logger.debug("updateHost｜req|{}|response code|{}|content|{}", body, response.code, data)
                if (!response.isSuccessful) {
                    logger.error("updateHost｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return
                }

                val resp = objectMapper.readValue<UpdateHostResp>(data)
                if (!resp.result || resp.code > 0) {
                    logger.error("updateHost｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return
                }
            }
        } catch (e: Exception) {
            logger.error("updateHost request error", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BKCCService::class.java)
    }
}


data class UpdateHostReqBody(
    @JsonProperty("bk_app_code")
    val bkAppCode: String,
    @JsonProperty("bk_app_secret")
    val bkAppSecret: String,
    @JsonProperty("bk_host_id")
    val bkHostId: String,
    val data: Map<String, Any>
)

data class UpdateHostResp(
    val result: Boolean,
    val code: Int,
    val message: String,
    @JsonProperty("request_id")
    val requestId: String
)