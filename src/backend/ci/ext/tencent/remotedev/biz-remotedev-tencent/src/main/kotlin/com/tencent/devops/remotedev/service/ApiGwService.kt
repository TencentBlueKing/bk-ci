package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.pojo.ProjectAccessDevicePermissionsResp
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class ApiGwService @Autowired constructor(
    private val bkConfig: BkConfig,
    private val objectMapper: ObjectMapper
) {
    fun projectAccessDevicePermissions(
        mac: String,
        userId: String,
        projects: String,
        ip: String
    ): Map<String, ProjectAccessDevicePermissionsResp>? {
        val url = "${bkConfig.remoteDevUrl}/apigw/v1/remote_dev/project_access_device_permissions/" +
                "?mac=$mac&account=$userId&project_codes=$projects&ip=$ip"
        val request = Request.Builder()
            .url(url)
            .get()
            .header(
                name = "X-Bkapi-Authorization",
                value = JsonUtil.toJson(
                    mapOf("bk_app_code" to bkConfig.appCode, "bk_app_secret" to bkConfig.appSecret),
                    false
                )
            )
            .build()
        return OkhttpUtils.doHttp(request).use { response ->
            val data = response.body!!.string()
            logger.debug("projectAccessDevicePermissions|{}|{}|{}", request.url, response.code, data)
            if (!response.isSuccessful) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.PROJECT_ACCESS_DEVICE_PERMISSION.errorCode,
                    defaultMessage = "request fail code ${response.code}"
                )
            }

            val resp = objectMapper.readValue<AccessDevicePermissionsResp>(data)
            if (!resp.result) {
                logger.error("projectAccessDevicePermissions|{}|{}|{}", request.url, response.code, data)
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.PROJECT_ACCESS_DEVICE_PERMISSION.errorCode,
                    defaultMessage = "code ${resp.code} msg ${resp.message}"
                )
            }
            resp.data.result
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiGwService::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessDevicePermissionsResp(
    val result: Boolean,
    val code: Int,
    val data: AccessDevicePermissionsRespData,
    val message: String?,
    @JsonProperty("request_id")
    val requestId: String?,
    @JsonProperty("traceId")
    val traceId: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessDevicePermissionsRespData(
    val result: Map<String, ProjectAccessDevicePermissionsResp>?
)