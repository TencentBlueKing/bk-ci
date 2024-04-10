package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.BkConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
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
        tData: Map<Long, Pair<String, Boolean>>
    ): String {
        val url = "${bkConfig.itsmHost}/v2/itsm/create_ticket"
        val body = BKItsmCreateTicketReq(
            bkAppCode = bkConfig.appCode,
            bkAppSecret = bkConfig.appSecret,
            serviceId = bkConfig.tgitLinkServiceId!!,
            creator = userId,
            fields = listOf(
                mapOf(
                    "key" to "title",
                    "value" to "$projectId|$userId"
                ),
                mapOf(
                    "key" to "bkci_project_id",
                    "value" to projectId
                ),
                mapOf(
                    "key" to "url",
                    "value" to tData.values.joinToString("\n") { it.first }
                ),
                mapOf(
                    "key" to "userId",
                    "value" to userId
                ),
                mapOf(
                    "key" to "tgit_ids",
                    "value" to tData.map { "${it.key};${it.value.first}" }.toSet().joinToString("\n")
                ),
                mapOf(
                    "key" to "tgit_ids_no_url",
                    "value" to tData.map { it.key.toString() }.toSet().joinToString(";")
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
                logger.debug("createTicket｜$url|$body|${response.code}|$data")
                if (!response.isSuccessful) {
                    logger.error("createTicket｜$url|$body|${response.code}|$data")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.CREATE_ITSM_TICKET_ERROR.errorCode,
                        params = arrayOf(projectId, userId)
                    )
                }

                val resp = objectMapper.readValue<BKItsmCreateTicketResp<BKItsmCreateTicketRespData>>(data)
                if (!resp.result) {
                    logger.error("createTicket｜$url|$body|${response.code}|$data")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.CREATE_ITSM_TICKET_ERROR.errorCode,
                        params = arrayOf(projectId, userId)
                    )
                }
                resp
            }
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.error("createTicket request error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.CREATE_ITSM_TICKET_ERROR.errorCode,
                params = arrayOf(projectId, userId)
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
