package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.dispatch.devcloud.pojo.devcloud.JobResponse
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.builds.BuildDevCloudResource
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.DevCloudJobReq
import com.tencent.devops.dispatch.devcloud.service.DispatchDevcloudService

@RestResource
class BuildDevCloudResourceImpl constructor(
    private val dispatchDevcloudService: DispatchDevcloudService
) : BuildDevCloudResource {
    override fun createJob(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobReq: DevCloudJobReq
    ): Result<JobResponse> {
        checkUserId(userId)
        return Result(dispatchDevcloudService.createJob(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            jobReq = jobReq
        ))
    }

    override fun getJobStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        jobName: String
    ): Result<String> {
        checkUserId(userId)
        return Result(dispatchDevcloudService.getJobStatus(userId, projectId, pipelineId, jobName))
    }

    override fun getJobLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        jobName: String
    ): Result<String> {
        checkUserId(userId)
        return Result(dispatchDevcloudService.getJobLogs(userId, projectId, pipelineId, jobName))
    }

    override fun getTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        taskId: String
    ): Result<String> {
        checkUserId(userId)
        return Result(dispatchDevcloudService.getTask(userId, projectId, pipelineId, taskId))
    }

    private fun checkUserId(userId: String) {
        if (userId.isEmpty()) {
            throw InvalidParamException("UserId can not be empty.")
        }
    }
}
