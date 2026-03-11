package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.DevCloudJobReq
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.JobResponse
import com.tencent.devops.dispatch.devcloud.utils.DevCloudJobRedisUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DevcloudJobService @Autowired constructor(
    private val dispatchDevCloudClient: com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient,
    private val devCloudJobRedisUtils: DevCloudJobRedisUtils
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DevcloudJobService::class.java)
    }

    fun createJob(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobReq: DevCloudJobReq
    ): JobResponse {
        logger.info("【**】projectId: $projectId, buildId: $buildId create devCloud jobContainer. userId: $userId")
        // 检查job数量是否超出限制

        val containerName = jobReq.podNameSelector!!.split("-").first()

        if (devCloudJobRedisUtils.getJobCount(buildId, containerName) > 10) {
            throw ErrorCodeException(
                statusCode = 500,
                errorCode = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.formatErrorMessage
            )
        }
        devCloudJobRedisUtils.setJobCount(buildId, containerName)

        return dispatchDevCloudClient.createJob(userId, projectId, pipelineId, buildId, jobReq)
    }

    fun getJobStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        jobName: String
    ): String {
        return dispatchDevCloudClient.getJobStatus(userId, projectId, pipelineId, jobName)
    }

    fun getJobLogs(
        projectId: String,
        pipelineId: String,
        userId: String,
        jobName: String
    ): String {
        return dispatchDevCloudClient.getJobLogs(userId, projectId, pipelineId, jobName)
    }

    fun getTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        taskId: String
    ): String {
        return dispatchDevCloudClient.getTasks(projectId, pipelineId, userId, taskId).toString()
    }
}
