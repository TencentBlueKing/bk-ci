package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.environment.permission.impl.TxV3EnvironmentPermissionService.Companion.logger
import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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
        val client = OkHttpClient()
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
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        print("[executeScript] responseBody msg: ${responseBody}")

        val scriptExecuteResult = ScriptExecuteResult(111, "111", 111)
        return scriptExecuteResult
    }
}