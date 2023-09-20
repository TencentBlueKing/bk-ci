package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.Host
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobInstance
import com.tencent.devops.environment.pojo.job.JobStepInstance
import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.StepHostResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
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
        returnIpResult: Boolean
    ): Result<QueryJobInstanceStatusResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "queryJobInstanceStatus",
                operationEnv = "stag",
                bkUsername = userId
            )

        val request = NetworkUtil.createGetRequest(
            url = jobCloudAuthenticationReq.url + String.format(
                QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX,
                jobCloudAuthenticationReq.bkScopeType,
                jobCloudAuthenticationReq.bkScopeId,
                jobInstanceId,
                returnIpResult
            ),
            bkAuthorization = jobCloudAuthenticationReq.bkAuthorization
        )
        val jobCloudResp: JobCloudResp<QueryJobInstanceStatusResult> =
            NetworkUtil.executeHttpRequest("queryJobInstanceStatus", request)
        val queryJobInstanceStatusResult = QueryJobInstanceStatusResult(
            finished = jobCloudResp.data?.finished ?: false,
            jobInstance = jobCloudResp.data?.jobInstance ?: JobInstance(
                name = "",
                status = -1,
                createTime = 0,
                startTime = 0,
                endTime = 0,
                totalTime = 0,
                jobInstanceId = -1
            ),
            stepInstanceList = jobCloudResp.data?.stepInstanceList?.map {
                JobStepInstance(
                    stepInstanceId = -1,
                    type = -1,
                    name = "",
                    stepStatus = -1,
                    createTime = -1,
                    startTime = -1,
                    endTime = -1,
                    totalTime = -1,
                    stepRetries = -1,
                    stepHostResultList = it.stepHostResultList.map {
                        StepHostResult(
                            host = Host(
                                bkCloudId = -1,
                                ip = "",
                                bkHostId = -1
                            ),
                            status = -1,
                            tag = "",
                            exitCode = -1,
                            errorCode = -1,
                            startTime = -1,
                            endTime = -1,
                            totalTime = -1
                        )
                    }
                )
            }
        )
        return Result(
            status = jobCloudResp.code,
            message = jobCloudResp.message,
            data = queryJobInstanceStatusResult
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QueryJobInstanceStatusService::class.java)
        private const val QUERY_JOB_INSTANCE_STATUS_URL_SUFFIX =
            "/?bk_scope_type=%s&bk_scope_id=%s&job_instance_id=%s&return_ip_result=%s"
    }
}