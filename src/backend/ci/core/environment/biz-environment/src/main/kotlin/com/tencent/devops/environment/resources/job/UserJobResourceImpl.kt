package com.tencent.devops.environment.resources.job

import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.job.UserJobResource
import com.tencent.devops.environment.pojo.job.req.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.resp.JobResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.service.job.JobService
import com.tencent.devops.environment.service.job.PermissionManageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserJobResourceImpl @Autowired constructor(
    private val jobService: JobService,
    private val permissionManageService: PermissionManageService
) : UserJobResource {
    override fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        checkParamBlank(userId, projectId)
        checkJobInsBelongToProj(projectId, jobInstanceId)
        val jobResult = jobService.queryJobInstanceStatus(projectId, jobInstanceId, returnIpResult)
        jobResult.data?.let { it.jobInstance?.let { it1 -> recordJobInsToProj(projectId, it1.jobInstanceId, userId) } }
        return jobResult
    }

    override fun queryJobInstanceLogs(
        userId: String,
        projectId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        checkParamBlank(userId, projectId)
        checkJobInsBelongToProj(projectId, queryJobInstanceLogsReq.jobInstanceId)
        val jobResult = jobService.queryJobInstanceLogs(queryJobInstanceLogsReq)
        jobResult.data?.let { recordJobInsToProj(projectId, it.jobInstanceId, userId) }
        return jobResult
    }

    private fun checkParamBlank(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("userId is blank.")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("projectId is blank.")
        }
    }

    private fun checkJobInsBelongToProj(projectId: String, jobInstanceId: Long) {
        if (!permissionManageService.isJobInsBelongToProj(projectId, jobInstanceId)) {
            throw OauthForbiddenException(
                message = "The job instance you have queried doesn't belong to the current project " +
                    "or more than three months."
            )
        }
    }

    private fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        permissionManageService.recordJobInsToProj(projectId, jobInstanceId, createUser)
    }
}