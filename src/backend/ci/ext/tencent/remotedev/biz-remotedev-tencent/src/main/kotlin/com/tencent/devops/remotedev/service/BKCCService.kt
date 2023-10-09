package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
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
    private val objectMapper: ObjectMapper,
    private val workspaceCommon: WorkspaceCommon
) {

    @Value("\${bkCC.host:}")
    val ccHost: String = ""

    @Value("\${bkCC.userName:}")
    val userName: String = ""

    @Value("\${bkCC.bizId:#{null}}")
    val bizId: Int? = null

    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    fun updateHostMonitor(
        regionId: Int?,
        workspaceName: String?,
        ips: Set<String>,
        props: Map<String, Any>
    ) {
        if (regionId == null && workspaceName.isNullOrBlank()) {
            logger.warn("updateHostMonitor regionId and workspaceName is null")
            return
        }

        // 先拿云区域 id
        var regId = regionId
        if (regId == null) {
            regId = workspaceCommon.getWorkspaceDetail(workspaceName!!)?.regionId
            if (regId == null) {
                logger.warn("update $workspaceName but regionid is null")
                return
            }
        }

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
        val hostIds = listBizHosts(
            fields = listOf(element = "bk_host_id"),
            page = page,
            hostPropertyFilter = filter
        )?.info?.map { it["bk_host_id"].toString() }?.toSet()
        if (hostIds.isNullOrEmpty()) {
            logger.warn("updateHostMonitor|$regId|$workspaceName|$ips hostids is empty")
            return
        }

        // 更新主机信息
        updateHost(hostIds, props)
    }

    fun updateHost(
        hostIds: Set<String>,
        props: Map<String, Any>
    ) {
        logger.info("updateHost|hostIds|$hostIds|props|$props")
        val url = "$ccHost/update_host/"
        val body = UpdateHostReqBody(
            bkAppCode = appCode,
            bkAppSecret = appSecret,
            bkUserName = userName,
            bkHostId = hostIds.joinToString(separator = ","),
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

    companion object {
        private val logger = LoggerFactory.getLogger(BKCCService::class.java)
    }

    fun listBizHosts(
        fields: List<String>,
        page: ListBizHostsReqPage,
        hostPropertyFilter: ListBizHostsCond
    ): ListBizHostResp? {
        if (bizId == null) {
            return null
        }
        val url = "$ccHost/list_biz_hosts/"
        val body = ListBizHostsReqBody(
            bkAppCode = appCode,
            bkAppSecret = appSecret,
            bkUserName = userName,
            page = page,
            bkBizId = bizId!!,
            fields = fields,
            hostPropertyFilter = hostPropertyFilter
        )
        val request = Request.Builder()
            .url(url)
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
}

data class UpdateHostReqBody(
    @JsonProperty("bk_app_code")
    val bkAppCode: String,
    @JsonProperty("bk_app_secret")
    val bkAppSecret: String,
    @JsonProperty("bk_username")
    val bkUserName: String,
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
    @JsonProperty("bk_app_code")
    val bkAppCode: String,
    @JsonProperty("bk_app_secret")
    val bkAppSecret: String,
    @JsonProperty("bk_username")
    val bkUserName: String,
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
