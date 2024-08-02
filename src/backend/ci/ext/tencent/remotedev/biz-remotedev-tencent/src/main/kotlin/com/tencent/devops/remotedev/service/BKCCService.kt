package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.config.BkConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 封装 bkcc 相关接口
 */
@Service
class BKCCService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val bkConfig: BkConfig
) {
    fun updateHostMonitor(
        regionId: Int,
        ip: String,
        props: Map<String, Any>
    ) {
        val (page, filter) = genHostIdFilter(regionId, setOf(ip))

        val hostIds = listBizHosts(
            fields = listOf(element = "bk_host_id"),
            page = page,
            hostPropertyFilter = filter
        )?.info?.map { it["bk_host_id"].toString() }?.toSet()
        if (hostIds.isNullOrEmpty()) {
            logger.warn("updateHostMonitor|$regionId|$ip hostids is empty")
            return
        }

        // 更新主机信息
        updateHost(hostIds, props)
    }

    private fun genHostIdFilter(
        regId: Int,
        ips: Set<String>
    ): Pair<ListBizHostsReqPage, ListBizHostsCond> {
        // 通过云区域 id 和 ip 获取 hostId
        val rule1 = if (ips.size == 1) {
            ListBizHostsCondRules(
                field = "bk_host_innerip",
                operator = "equal",
                value = ips.first()
            )
        } else {
            ListBizHostsCondRules(
                field = "bk_host_innerip",
                operator = "in",
                value = ips
            )
        }
        val filter = ListBizHostsCond(
            condition = "AND",
            rules = listOf(
                rule1,
                ListBizHostsCondRules(
                    field = "bk_cloud_id",
                    operator = "equal",
                    value = regId
                )
            )
        )
        val page = ListBizHostsReqPage(
            start = 0,
            limit = ips.size + 1,
            sort = "bk_host_id"
        )

        return Pair(page, filter)
    }

    fun updateHost(
        hostIds: Set<String>,
        props: Map<String, Any>
    ) {
        logger.debug("updateHost|hostIds|{}|props|{}", hostIds, props)
        val url = "${bkConfig.ccHost}/update_host/"
        val body = UpdateHostReqBody(
            bkHostId = hostIds.joinToString(separator = ","),
            data = props
        )
        val request = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr())
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

                val resp = objectMapper.readValue<UpdateHostResp<Any>>(data)
                if (!resp.result || resp.code > 0) {
                    logger.error("updateHost｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return
                }
            }
        } catch (e: Exception) {
            logger.error("updateHost request error", e)
        }
    }

    fun listBizHosts(
        fields: List<String>,
        page: ListBizHostsReqPage,
        hostPropertyFilter: ListBizHostsCond
    ): ListBizHostResp? {
        if (bkConfig.ccBizId == null) {
            return null
        }
        val url = "${bkConfig.ccHost}/list_biz_hosts/"
        val body = ListBizHostsReqBody(
            page = page,
            bkBizId = bkConfig.ccBizId!!,
            fields = fields,
            hostPropertyFilter = hostPropertyFilter
        )
        val request = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr())
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                logger.debug("listBizHosts｜req|{}|response code|{}|content|{}", body, response.code, data)
                if (!response.isSuccessful) {
                    logger.error("listBizHosts｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }

                val resp = objectMapper.readValue<UpdateHostResp<ListBizHostResp>>(data)
                if (!resp.result || resp.code > 0) {
                    logger.error("listBizHosts｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }

                return resp.data
            }
        } catch (e: Exception) {
            logger.error("listBizHosts request error", e)
        }

        return null
    }
    private fun headerStr(): String {
        return objectMapper.writeValueAsString(
            mapOf(
                "bk_app_code" to bkConfig.appCode,
                "bk_app_secret" to bkConfig.appSecret,
                "bk_username" to bkConfig.ccUserName
            )
        ).replace("\\s".toRegex(), "")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BKCCService::class.java)
    }
}

data class UpdateHostReqBody(
    @JsonProperty("bk_host_id")
    val bkHostId: String,
    val data: Map<String, Any>
)

data class UpdateHostResp<T>(
    val result: Boolean,
    val code: Int,
    val message: String,
    @JsonProperty("request_id")
    val requestId: String,
    val data: T?
)

data class ListBizHostsReqBody(
    val page: ListBizHostsReqPage,
    @JsonProperty("bk_biz_id")
    val bkBizId: Int,
    val fields: List<String>,
    @JsonProperty("host_property_filter")
    val hostPropertyFilter: ListBizHostsCond
)

data class ListBizHostsReqPage(
    val start: Int,
    val limit: Int,
    val sort: String
)

data class ListBizHostsCond(
    val condition: String,
    val rules: List<ListBizHostsCondRules>
)

data class ListBizHostsCondRules(
    val field: String,
    val operator: String,
    val value: Any
)

data class ListBizHostResp(
    val count: Int,
    val info: List<Map<String, Any>>
)
