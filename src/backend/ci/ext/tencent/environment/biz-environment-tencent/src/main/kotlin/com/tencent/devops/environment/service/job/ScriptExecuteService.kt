package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.ScriptExecuteJobCloudReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.utils.job.NetworkUtil
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

        val request = NetworkUtil.createPostRequest(
            url = scriptExecuteUrl,
            bkAuthorization = bkAuthorization,
            jobCloudReq = scriptExecuteJobCloudReq::class.java
        )
        val jobCloudResp = NetworkUtil.executeHttpRequest("executeScript", request)
        val scriptExecuteResult = ScriptExecuteResult(
            jobInstanceId = jobCloudResp.data?.jobInstanceId ?: 0,
            jobInstanceName = jobCloudResp.data?.jobInstanceName ?: "",
            stepInstanceId = jobCloudResp.data?.stepInstanceId ?: 0
        )
        return Result(
            status = jobCloudResp.code,
            message = jobCloudResp.message,
            data = scriptExecuteResult
        )
    }
}