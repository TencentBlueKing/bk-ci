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
    fun createLinkTicket(
        projectId: String,
        userId: String,
        tData: Map<Long, Pair<String, Boolean>>,
        index: Int?
    ): String {
        return createTicket(
            projectId = projectId,
            creator = userId,
            fields = listOf(
                mapOf(
                    "key" to "title",
                    "value" to "云研发项目关联工蜂仓库|$projectId|$userId|${index ?: ""}"
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
    }

    fun createCheckTicket(
        projectId: String,
        creator: String,
        operator: String,
        urls: List<String>
    ) {
        val fields = listOf(
            mapOf(
                "key" to "title",
                "value" to "检测到云控制台有${urls.size}个工蜂仓库绑定状态异常，请及时处理！"
            ),
            mapOf(
                "key" to "projectId",
                "value" to projectId
            ),
            mapOf(
                "key" to "OPERATOR",
                "value" to operator
            ),
            mapOf(
                "key" to "YCDXMLB",
                "value" to urls.joinToString("\n")
            )
        )
        createTicket(projectId, creator, fields)
    }

    fun createTicket(
        projectId: String,
        creator: String,
        fields: List<Map<String, String>>
    ): String {
        val url = "${bkConfig.itsmHost}/v2/itsm/create_ticket"
        val body = BKItsmCreateTicketReq(
            serviceId = bkConfig.tgitLinkServiceId!!,
            creator = creator,
            fields = fields
        )
        val request = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr())
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
                        params = arrayOf(projectId, creator)
                    )
                }

                val resp = objectMapper.readValue<BKItsmCreateTicketResp<BKItsmCreateTicketRespData>>(data)
                if (!resp.result) {
                    logger.error("createTicket｜$url|$body|${response.code}|$data")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.CREATE_ITSM_TICKET_ERROR.errorCode,
                        params = arrayOf(projectId, creator)
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
                params = arrayOf(projectId, creator)
            )
        }

        return resp.data.sn
    }

    private fun headerStr(): String {
        return objectMapper.writeValueAsString(
            mapOf("bk_app_code" to bkConfig.appCode, "bk_app_secret" to bkConfig.appSecret)
        ).replace("\\s".toRegex(), "")
    }
    companion object {
        private val logger = LoggerFactory.getLogger(BKItsmService::class.java)
    }
}

data class BKItsmCreateTicketReq(
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
