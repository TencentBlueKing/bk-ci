package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.org.slf4j.internal.LoggerFactory
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.config.BkConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BKItsmService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val bkConfig: BkConfig
) {

    fun createTicket(
        projectId: String,
        userId: String,
        urls: Set<String>
    ): String {
        val url = "${bkConfig.itsmHost}/v2/itsm/create_ticket"
        val body = BKItsmCreateTicketReq(
            bkAppCode = bkConfig.appCode,
            bkAppSecret = bkConfig.appSecret,
            serviceId = bkConfig.tgitLinkServiceId!!,
            creator = userId,
            fields = listOf(
                mapOf(
                    "key" to "bkci_project_id",
                    "value" to projectId
                ),
                mapOf(
                    "key" to "url",
                    "value" to urls.joinToString { "\n" }
                )
            )
        )
        val request = Request.Builder()
            .url(url)
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        val resp = try {
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                logger.debug("createTicket｜req|{}|response code|{}|content|{}", body, response.code, data)
                if (!response.isSuccessful) {
                    logger.error("createTicket｜req|{}|response code|{}|content|{}", body, response.code, data)
                    // TODO: 创建单据失败错误
                    throw ErrorCodeException(
                        errorCode = "0",
                        errorType = ErrorType.USER
                    )
                }

                val resp = objectMapper.readValue<BKItsmCreateTicketResp<BKItsmCreateTicketRespData>>(data)
                if (!resp.result) {
                    logger.error("createTicket｜req|{}|response code|{}|content|{}", body, response.code, data)
                    // TODO: 创建单据失败错误
                    throw ErrorCodeException(
                        errorCode = "0",
                        errorType = ErrorType.USER
                    )
                }
                resp
            }
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.error("createTicket request error", e)
            // TODO: 创建单据失败错误
            throw ErrorCodeException(
                errorCode = "0",
                errorType = ErrorType.USER
            )
        }

        return resp.data.sn
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BKItsmService::class.java)
    }
}

data class BKItsmCreateTicketReq(
    @JsonProperty("bk_app_code")
    val bkAppCode: String,
    @JsonProperty("bk_app_secret")
    val bkAppSecret: String,
    @JsonProperty("service_id")
    val serviceId: Int,
    @JsonProperty("bkdata_data_token")
    val creator: String,
    // [{"key": "title", "value": "d" }]
    val fields: List<Map<String, String>>
)

data class BKItsmCreateTicketResp<T>(
    val result: Boolean,
    val message: String,
    val code: String,
    val data: T
)

data class BKItsmCreateTicketRespData(
    val sn: String
)
