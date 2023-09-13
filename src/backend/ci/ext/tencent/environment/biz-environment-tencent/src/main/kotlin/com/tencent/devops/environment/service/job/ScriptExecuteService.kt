package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.permission.impl.TxV3EnvironmentPermissionService.Companion.logger
import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException

@Service("ScriptExecuteService")
class ScriptExecuteService {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""
    fun executeScript(
        userId: String,
        projectId: String,
        scriptExecuteReq: ScriptExecuteReq
    ): ScriptExecuteResult {
        val bkAppCode = appCode
        val bkAppSecret = appSecret
        val access_token = "C7Lr9b3Eopjf456446lCTiNN3rgg99" // TODO：改为配置项
        val scriptExecuteUrl = "https://jobv3-cloud.apigw.o.woa.com/prod/api/v3/fast_execute_script/" // TODO：改为配置项
        val scriptExecuteReqBody = mapOf(
            "bk_scope_type" to "biz", // TODO：改为配置项
            "bk_scope_id" to "309", // TODO：改为配置项
            "script_content" to scriptExecuteReq.scriptContent,
            "script_param" to scriptExecuteReq.scriptParam,
            "timeout" to scriptExecuteReq.timeout,
            "account_id" to scriptExecuteReq.account,
            "is_param_sensitive" to scriptExecuteReq.isSensiveParam,
            "script_language" to scriptExecuteReq.scriptType,
            "target_server" to scriptExecuteReq.executeTarget,
        )
        val request = Request.Builder()
            .url(scriptExecuteUrl)
            .post(
                RequestBody.create(
                    "application/json;charset=utf-8".toMediaTypeOrNull(),
                    ObjectMapper().writeValueAsString(scriptExecuteReqBody)
                )
            )
            .addHeader("Content-Type", "application/json")
            .addHeader(
                "X-Bkapi-Authorization",
                "{\"bk_app_code\": \"${bkAppCode}\", " +
                    "\"bk_app_secret\": \"${bkAppSecret}\", " +
                    "\"access_token\": \"${access_token}\"}"
            )
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body?.string()
                logger.info("[executeScript] responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                val jobRequestId = responseData["job_request_id"] as String
                if (false == responseData["result"]) {
                    val errorMsg = responseData["message"]
                    logger.error("[executeScript] Execute failed! Req ID: ${jobRequestId}, Error msg: ${errorMsg}")
                    throw RemoteServiceException("Execute script failed! Error message: ${errorMsg}")
                }
                val data = responseData["data"] as Map<*, *>
                val jobInstanceId = data["job_instance_id"] as Long
                val jobInstanceName = data["job_instance_name"] as String
                val stepInstanceId = data["step_instance_id"] as Long

                return ScriptExecuteResult(
                    jobInstanceId = jobInstanceId,
                    jobInstanceName = jobInstanceName,
                    stepInstanceId = stepInstanceId
                )
            } catch (exception: Exception) {
                logger.error("[executeScript] Execute script error: ${exception}")
                when (exception) {
                    is IOException ->
                        throw IOException("Connection or server exception：${exception}")

                    is JSONException ->
                        throw JSONException("Parse json response exception：${exception}")

                    else ->
                        throw Exception("Execute script exception occur：${exception}")
                }
            }
        }
    }
}