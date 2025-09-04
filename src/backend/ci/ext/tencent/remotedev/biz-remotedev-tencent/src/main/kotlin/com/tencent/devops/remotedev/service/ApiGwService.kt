package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.pojo.ProjectAccessDevicePermissionsResp
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
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
            .headers(genCommonHeader())
            .build()
        return OkhttpUtils.doHttp(request).use { response ->
            val data = response.body!!.string()
            logger.info("projectAccessDevicePermissions|{}|{}|{}", request.url, response.code, data)
            if (!response.isSuccessful) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.PROJECT_ACCESS_DEVICE_PERMISSION.errorCode,
                    defaultMessage = "request fail code ${response.code}"
                )
            }

            val resp = objectMapper.readValue<RemoteDevApiGwResp<AccessDevicePermissionsRespData>>(data)
            if (!resp.result) {
                logger.error("projectAccessDevicePermissions|{}|{}|{}", request.url, response.code, data)
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.PROJECT_ACCESS_DEVICE_PERMISSION.errorCode,
                    defaultMessage = "code ${resp.code} msg ${resp.message}"
                )
            }
            resp.data?.result
        }
    }

    fun workspaceAccessManageControl(
        projectId: String,
        workspaceName: String
    ): UserInfoCheckResult? {
        val url = "${bkConfig.remoteDevUrl}/apigw/v1/remote_dev/workspace_access_manage_control" +
                "?project_code=$projectId&workspace_name=$workspaceName"
        val request = Request.Builder()
            .url(url)
            .get()
            .headers(genCommonHeader())
            .build()
        return OkhttpUtils.doHttp(request).use { response ->
            val data = response.body!!.string()
            logger.info("workspaceAccessManageControl|{}|{}|{}", request.url, response.code, data)
            if (!response.isSuccessful) {
                throw RemoteServiceException("request $url fail, code ${response.code}", response.code)
            }

            val resp = objectMapper.readValue<RemoteDevApiGwResp<UserInfoCheckResult>>(data)
            if (!resp.result) {
                logger.error("workspaceAccessManageControl|{}|{}|{}", request.url, response.code, data)
                throw RemoteServiceException("request $url error, code ${resp.code} msg ${resp.message}", response.code)
            }
            resp.data
        }
    }

    fun genCommonHeader(): Headers {
        val res = mapOf(
            "X-Bkapi-Authorization" to
                    """{"bk_app_code":"${bkConfig.appCode}","bk_app_secret":"${bkConfig.appSecret}"}"""
        )
        return res.toHeaders()
    }

    // 调用wesec接口判断该项目+云桌面是否开启moa 2fa管控
    fun checkMoa2fa(
        project: String,
        workspactName: String
    ): Boolean? {
        val url = "${bkConfig.remoteDevUrl}/apigw/v1/remote_dev/moa_verify/" +
            "?project_code=$project&workspace_name=$workspactName"
        val request = Request.Builder()
            .url(url)
            .get()
            .headers(genCommonHeader())
            .build()
        return OkhttpUtils.doHttp(request).use { response ->
            val data = response.body!!.string()
            logger.debug("checkMoa2fa|{}|{}|{}", request.url, response.code, data)
            if (!response.isSuccessful) {
                logger.error("checkMoa2fa|{}|{}|{}", request.url, response.code, data)
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.MOA_VERIRY.errorCode,
                    defaultMessage = "request fail code ${response.code}"
                )
            }

            val resp = objectMapper.readValue<RemoteDevApiGwResp<MoaVerifyRespData>>(data)
            if (!resp.result) {
                logger.error("checkMoa2fa|{}|{}|{}", request.url, response.code, data)
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.MOA_VERIRY.errorCode,
                    defaultMessage = "code ${resp.code} msg ${resp.message}"
                )
            }
            resp.data?.result
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiGwService::class.java)
    }
}

// permission check相关
@JsonIgnoreProperties(ignoreUnknown = true)
data class RemoteDevApiGwResp<T>(
    val result: Boolean,
    val code: Int,
    val data: T?,
    val message: String?,
    @JsonProperty("request_id")
    val requestId: String?,
    @JsonProperty("trace_id")
    val traceId: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessDevicePermissionsRespData(
    val result: Map<String, ProjectAccessDevicePermissionsResp>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MoaVerifyRespData(
    val result: Boolean?
)
