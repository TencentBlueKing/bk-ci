package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("QueryJobInstanceStatusService")
class QueryJobInstanceStatusService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    val logger = LoggerFactory.getLogger(QueryJobInstanceStatusService::class.java)
    fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        AuthenticationService.set("queryJobInstanceStatus")
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = authenticationService.appAuthentication(userId)
        val jobCloudResp: JobCloudResp<QueryJobInstanceStatusResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "get",
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
        val queryJobInstanceStatusResult: QueryJobInstanceStatusResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                QueryJobInstanceStatusResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[queryJobInstanceStatus] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[queryJobInstanceStatus] serialized jsonData: $jsonData")
            logger.debug("[queryJobInstanceStatus] queryJobInsStatusResult: $queryJobInstanceStatusResult")
        }

        return JobResult(
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