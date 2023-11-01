package com.tencent.devops.environment.resources.job

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.environment.api.job.UserJobResource
import com.tencent.devops.environment.pojo.job.req.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.resp.JobResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.service.job.JobService
import org.springframework.beans.factory.annotation.Autowired

class UserJobResourceImpl @Autowired constructor(
    private val jobService: JobService
) : UserJobResource {
    override fun queryJobInstanceStatus(
        userId: String,
        projectId: String,
        jobInstanceId: Long,
        returnIpResult: Boolean?
    ): JobResult<QueryJobInstanceStatusResult> {
        checkParam(userId, projectId)
        return jobService.queryJobInstanceStatus(userId, projectId, jobInstanceId, returnIpResult)
    }

    override fun queryJobInstanceLogs(
        userId: String,
        projectId: String,
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult> {
        checkParam(userId, projectId)
        return jobService.queryJobInstanceLogs(userId, queryJobInstanceLogsReq)
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("userId is blank.")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("projectId is blank.")
        }
    }
}