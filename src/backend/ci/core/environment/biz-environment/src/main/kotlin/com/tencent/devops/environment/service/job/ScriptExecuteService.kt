package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.req.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import com.tencent.devops.environment.pojo.job.req.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("ScriptExecuteService")
class ScriptExecuteService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScriptExecuteService::class.java)
    }
    fun executeScript(jobCloudScriptExecuteReq: JobCloudScriptExecuteReq): JobResult<ScriptExecuteResult> {
        AuthenticationService.set("executeScript")
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(jobCloudScriptExecuteReq.bkUsername)
        jobCloudScriptExecuteReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudScriptExecuteReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<ScriptExecuteResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudScriptExecuteReq
            )

        var jsonData = ""
        val scriptExecuteResult: ScriptExecuteResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                ScriptExecuteResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[executeScript] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[executeScript] serialized jsonData: $jsonData")
            logger.debug("[executeScript] scriptExecuteResult: $scriptExecuteResult")
        }

        return JobResult(
            code = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = scriptExecuteResult
        )
    }
}