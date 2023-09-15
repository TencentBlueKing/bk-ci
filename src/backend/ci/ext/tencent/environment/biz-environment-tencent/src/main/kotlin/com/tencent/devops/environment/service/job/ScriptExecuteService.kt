package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.permission.impl.TxV3EnvironmentPermissionService.Companion.logger
import com.tencent.devops.environment.pojo.job.ScriptExecuteJobCloudReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteJobCloudResp
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("ScriptExecuteService")
class ScriptExecuteService {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""
    fun executeScript(scriptExecuteJobCloudReq: ScriptExecuteJobCloudReq): Result<ScriptExecuteResult> {
        val bkAppCode = appCode
        val bkAppSecret = appSecret
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"userId\": \"${scriptExecuteJobCloudReq.bk_username}\"}"
//        val scriptExecuteUrl = "https://jobv3-cloud.apigw.o.woa.com/prod/api/v3/fast_execute_script/" // 正式 TODO：改为配置项
        val scriptExecuteUrl = "https://jobv3-cloud.apigw.o.woa.com/stag/api/v3/fast_execute_script/" // 预发布 TODO：改为配置项
        scriptExecuteJobCloudReq.bk_app_code = appCode!!
        scriptExecuteJobCloudReq.bk_app_secret = appSecret!!
//        scriptExecuteJobCloudReq.bk_scope_type = bkScopeType!! // TODO：改为配置项
//        scriptExecuteJobCloudReq.bk_scope_id = bkScopeId!! // TODO：改为配置项
        val request = Request.Builder()
            .url(scriptExecuteUrl)
            .post(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(),
                    ObjectMapper().writeValueAsString(scriptExecuteJobCloudReq)
                )
            )
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Bkapi-Authorization", bkAuthorization)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body?.string()
                logger.info("[executeScript] responseBody: $responseBody")

                val scriptExecuteResp = jacksonObjectMapper().readValue<ScriptExecuteJobCloudResp>(responseBody!!)

                if (!scriptExecuteResp.result) {
                    logger.error(
                        "[executeScript] Execute failed! Req ID: ${scriptExecuteResp.jobRequestId}, " +
                            "Error code: ${scriptExecuteResp.code}, " +
                            "Error msg: ${scriptExecuteResp.message}"
                    )
                    throw RemoteServiceException(
                        "Execute script failed! Req ID: ${scriptExecuteResp.jobRequestId}, " +
                            "Error code: ${scriptExecuteResp.code}, " +
                            "Error msg: ${scriptExecuteResp.message}"
                    )
                }
                val scriptExecuteResult = ScriptExecuteResult(
                    jobInstanceId = scriptExecuteResp.data?.jobInstanceId ?: 0,
                    jobInstanceName = scriptExecuteResp.data?.jobInstanceName ?: "",
                    stepInstanceId = scriptExecuteResp.data?.stepInstanceId ?: 0
                )
                return Result(
                    status = scriptExecuteResp.code,
                    message = scriptExecuteResp.message,
                    data = scriptExecuteResult
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                throw exception
            }
        }
    }
}