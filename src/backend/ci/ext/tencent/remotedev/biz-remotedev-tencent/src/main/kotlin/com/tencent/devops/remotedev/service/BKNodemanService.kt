package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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

@Service
class BKNodemanService @Autowired constructor(
    private val bkConfig: BkConfig,
    private val objectMapper: ObjectMapper
) {
    fun ipchooserHostDetail(
        ip: String,
        cloudId: Int
    ): IpchooserHostDetailReqResp? {
        val url = "${bkConfig.bknodemanHost}/prod/core/api/ipchooser_host/details/"
        val body = IpchooserHostDetailReq(
            hostList = listOf(
                IpchooserHostDetailReqHost(
                    ip = ip,
                    cloudId = cloudId,
                    meta = IpchooserHostDetailReqHostMeta(
                        scopeType = "biz",
                        scopeId = bkConfig.bknodemanBizId.toString(),
                        bkBizId = bkConfig.bknodemanBizId ?: 0
                    )
                )
            ),
            scopeList = listOf(
                IpchooserHostDetailReqScope(
                    scopeType = "biz",
                    scopeId = bkConfig.bknodemanBizId.toString()
                )
            )
        )
        val request = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", bkConfig.headerStr())
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                logger.debug("ipchooserHostDetail｜req|{}|response code|{}|content|{}", body, response.code, data)
                if (!response.isSuccessful) {
                    logger.error("ipchooserHostDetail｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }

                val resp = objectMapper.readValue<NodeManResp<List<IpchooserHostDetailReqResp>>>(data)
                if (!resp.result || resp.code > 0) {
                    logger.error("ipchooserHostDetail｜req|{}|response code|{}|content|{}", body, response.code, data)
                    return null
                }

                return resp.data?.firstOrNull()
            }
        } catch (e: Exception) {
            logger.error("listBizHosts request error", e)
        }

        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BKNodemanService::class.java)
    }
}

data class IpchooserHostDetailReq(
    @JsonProperty("host_list")
    val hostList: List<IpchooserHostDetailReqHost>,
    @JsonProperty("scope_list")
    val scopeList: List<IpchooserHostDetailReqScope>
)

data class IpchooserHostDetailReqHost(
    val ip: String,
    @JsonProperty("cloud_id")
    val cloudId: Int,
    val meta: IpchooserHostDetailReqHostMeta
)

data class IpchooserHostDetailReqHostMeta(
    @JsonProperty("scope_type")
    val scopeType: String,
    @JsonProperty("scope_id")
    val scopeId: String,
    @JsonProperty("bk_biz_id")
    val bkBizId: Int
)

data class IpchooserHostDetailReqScope(
    @JsonProperty("scope_type")
    val scopeType: String,
    @JsonProperty("scope_id")
    val scopeId: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NodeManResp<T>(
    val result: Boolean,
    val code: Int,
    val message: String,
    val data: T?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IpchooserHostDetailReqResp(
    val alive: Int
)
