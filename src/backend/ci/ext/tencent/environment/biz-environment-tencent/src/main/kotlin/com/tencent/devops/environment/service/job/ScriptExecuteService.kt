package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import com.tencent.devops.environment.utils.job.NetworkUtil.logger
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
                operationEnv = "prod",
                bkUsername = jobCloudScriptExecuteReq.bkUsername
            )
        jobCloudScriptExecuteReq.bkAppCode = jobCloudAuthenticationReq.bkAppCode
        jobCloudScriptExecuteReq.bkAppSecret = jobCloudAuthenticationReq.bkAppSecret
        jobCloudScriptExecuteReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudScriptExecuteReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<ScriptExecuteResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                operateName = "executeScript",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudScriptExecuteReq.javaClass
            )
        logger.info("[executeScript] jobCloudResp: $jobCloudResp, type: ${jobCloudResp::class}")
        var scriptExecuteResult = ScriptExecuteResult(0L, "", 0L)
        if (null != jobCloudResp.data) {
            scriptExecuteResult = jacksonObjectMapper().readValue(jobCloudResp.data.toString())
        }
//        val scriptExecuteResult = ScriptExecuteResult(
//            jobInstanceId = jobCloudResp.data?.jobInstanceId ?: 0L,
//            jobInstanceName = jobCloudResp.data?.jobInstanceName ?: "",
//            stepInstanceId = jobCloudResp.data?.stepInstanceId ?: 0L
//        )

        return Result(
            status = jobCloudResp.code,
            message = jobCloudResp.message,
            data = scriptExecuteResult
        )
    }
}