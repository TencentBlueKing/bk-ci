package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryJobInstanceStatusService")
class QueryJobInstanceStatusService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): com.tencent.devops.environment.pojo.job.JobResult<com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "queryJobInstanceStatus",
                operationEnv = "prod",
                bkUsername = userId
            )

        val jobCloudResp: JobCloudResp<QueryJobInstanceStatusResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "get",
                operateName = "queryJobInstanceStatus",
                url = jobCloudAuthenticationReq.url + String.format(
                    QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX,
                    jobCloudAuthenticationReq.bkScopeType,
                    jobCloudAuthenticationReq.bkScopeId,
                    jobInstanceId,
                    returnIpResult
                ),
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = mapOf<String, Any>()
            )

        var jsonData = ""
        val queryJobInstanceStatusResult: com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult()
            }
        if (NetworkUtil.logger.isDebugEnabled) {
            NetworkUtil.logger.info("[queryJobInstanceStatus] jobCloudResp.data: ${jobCloudResp.data}")
            NetworkUtil.logger.info("[queryJobInstanceStatus] serialized jsonData: $jsonData")
            NetworkUtil.logger.info("[queryJobInstanceStatus] queryJobInsStatusResult: $queryJobInstanceStatusResult")
        }

        return com.tencent.devops.environment.pojo.job.JobResult(
            status = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = queryJobInstanceStatusResult
        )
    }

    companion object {
        private const val QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX =
            "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&return_ip_result=%s"
    }
}