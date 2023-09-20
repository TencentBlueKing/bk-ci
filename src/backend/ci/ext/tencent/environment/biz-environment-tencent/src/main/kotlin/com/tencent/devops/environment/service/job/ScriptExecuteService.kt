package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("ScriptExecuteService")
class ScriptExecuteService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    fun executeScript(jobCloudScriptExecuteReq: JobCloudScriptExecuteReq): Result<ScriptExecuteResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "executeScript",
                operationEnv = "stag",
                bkUsername = jobCloudScriptExecuteReq.bkUsername
            )
        jobCloudScriptExecuteReq.bkAppCode = jobCloudAuthenticationReq.bkAppCode
        jobCloudScriptExecuteReq.bkAppSecret = jobCloudAuthenticationReq.bkAppSecret
        jobCloudScriptExecuteReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudScriptExecuteReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val request = NetworkUtil.createPostRequest(
            url = jobCloudAuthenticationReq.url,
            bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
            jobCloudReq = jobCloudScriptExecuteReq::class.java
        )
        val jobCloudResp: JobCloudResp<ScriptExecuteResult> = NetworkUtil.executeHttpRequest("executeScript", request)
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