package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobCloudQueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryJobInstanceLogsService")
class QueryJobInstanceLogsService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    fun queryJobInstanceLogs(
        jobCloudQueryJobInstanceLogsReq: JobCloudQueryJobInstanceLogsReq
    ): com.tencent.devops.environment.pojo.job.JobResult<com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "queryJobInstanceLogs",
                operationEnv = "prod",
                bkUsername = jobCloudQueryJobInstanceLogsReq.bkUsername
            )
        jobCloudQueryJobInstanceLogsReq.bkAppCode = jobCloudAuthenticationReq.bkAppCode
        jobCloudQueryJobInstanceLogsReq.bkAppSecret = jobCloudAuthenticationReq.bkAppSecret
        jobCloudQueryJobInstanceLogsReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudQueryJobInstanceLogsReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<QueryJobInstanceLogsResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                operateName = "queryJobInstanceLogs",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudQueryJobInstanceLogsReq.toMap()
            )

        var jsonData = ""
        val queryJobInstanceLogsResult: com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult()
            }
        if (NetworkUtil.logger.isDebugEnabled) {
            NetworkUtil.logger.info("[queryJobInstanceLogs] jobCloudResp.data: ${jobCloudResp.data}")
            NetworkUtil.logger.info("[queryJobInstanceLogs] serialized jsonData: $jsonData")
            NetworkUtil.logger.info("[queryJobInstanceLogs] queryJobInstanceLogsResult: $queryJobInstanceLogsResult")
        }

        return com.tencent.devops.environment.pojo.job.JobResult(
            status = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = queryJobInstanceLogsResult
        )
    }
}