package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobCloudQueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryJobInstanceLogsService")
class QueryJobInstanceLogsService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    val logger = LoggerFactory.getLogger(QueryJobInstanceLogsService::class.java)
    fun queryJobInstanceLogs(
        jobCloudQueryJobInstanceLogsReq: JobCloudQueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "queryJobInstanceLogs",
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
        val queryJobInstanceLogsResult: QueryJobInstanceLogsResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                QueryJobInstanceLogsResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[queryJobInstanceLogs] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[queryJobInstanceLogs] serialized jsonData: $jsonData")
            logger.debug("[queryJobInstanceLogs] queryJobInstanceLogsResult: $queryJobInstanceLogsResult")
        }

        return JobResult(
            status = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = queryJobInstanceLogsResult
        )
    }
}