package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobCloudTaskTerminateReq
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.TaskTerminateResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("TaskTerminateService")
class TaskTerminateService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    val logger = LoggerFactory.getLogger(QueryJobInstanceStatusService::class.java)
    fun terminateTask(jobCloudTaskTerminateReq: JobCloudTaskTerminateReq): JobResult<TaskTerminateResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "terminateTask",
                bkUsername = jobCloudTaskTerminateReq.bkUsername
            )
        jobCloudTaskTerminateReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudTaskTerminateReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<TaskTerminateResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                operateName = "terminateTask",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudTaskTerminateReq.toMap()
            )

        var jsonData = ""
        val taskTerminateResult: TaskTerminateResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                TaskTerminateResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[terminateTask] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[terminateTask] serialized jsonData: $jsonData")
            logger.debug("[terminateTask] taskTerminateResult: $taskTerminateResult")
        }

        return JobResult(
            status = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = taskTerminateResult
        )
    }
}