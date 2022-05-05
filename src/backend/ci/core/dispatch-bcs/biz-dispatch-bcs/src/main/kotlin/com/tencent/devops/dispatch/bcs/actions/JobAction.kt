package com.tencent.devops.dispatch.bcs.actions

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.dispatch.bcs.client.BcsJobClient
import com.tencent.devops.dispatch.bcs.common.ErrorCodeEnum
import com.tencent.devops.dispatch.bcs.pojo.*
import com.tencent.devops.dispatch.bcs.pojo.bcs.*
import com.tencent.devops.dispatch.bcs.utils.BcsJobRedisUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class JobAction @Autowired constructor(
    private val bcsJobRedisUtils: BcsJobRedisUtils,
    private val bcsJobClient: BcsJobClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobAction::class.java)
    }

    @Value("\${bcs.resources.job.cpu}")
    var cpu: Double = 32.0

    @Value("\${bcs.resources.job.memory}")
    var memory: Int = 65535

    @Value("\${bcs.resources.job.disk}")
    var disk: Int = 500

    @Value("\${bcs.sleepEntrypoint}")
    val entrypoint: String = "sleep.sh"

    fun createJob(
        userId: String,
        projectId: String,
        buildId: String,
        jobReq: DispatchJobReq
    ): DispatchTaskResp {
        logger.info("projectId: $projectId, buildId: $buildId create bcs jobContainer. userId: $userId")

        // 检查job数量是否超出限制
        if (bcsJobRedisUtils.getJobCount(buildId,  jobReq.podNameSelector) > 10) {
            throw ErrorCodeException(
                statusCode = 500,
                errorCode = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.formatErrorMessage
            )
        }
        bcsJobRedisUtils.setJobCount(buildId, jobReq.podNameSelector)

        val job = with(jobReq) {
            BcsJob(
                name = alias,
                builderName = podNameSelector,
                shareDiskMountPath = mountPath,
                deadline = activeDeadlineSeconds,
                image = image,
                registry = registry,
                cpu = cpu,
                memory = memory,
                disk = disk,
                env = params?.env,
                command = params?.command,
                workDir = params?.workDir,
                nfs = params?.nfsVolume?.map { nfsVo ->
                    NfsConfig(
                        server = nfsVo.server,
                        path = nfsVo.path,
                        mountPath = nfsVo.mountPath
                    )
                }
            )
        }

        val result = bcsJobClient.createJob(userId, job)
        if (result.isNotOk() || result.data == null) {
            return DispatchTaskResp(
                result.data?.taskId,
                result.message
            )
        }
        return DispatchTaskResp(result.data!!.taskId)
    }

    fun getJobStatus(userId: String, jobName: String): DispatchBuildStatusResp {
        val result = bcsJobClient.getJobStatus(userId, jobName)
        if (result.isNotOk()) {
            return DispatchBuildStatusResp(
                status = DispatchBuildStatusEnum.failed.name,
                errorMsg = result.message
            )
        }
        val status = BcsJobStatusEnum.realNameOf(result.data?.status)
        if (status == null || status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status?.message)
        }
        return when {
            status.isSuccess() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            status.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }

    fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): DispatchJobLogResp {
        val result = bcsJobClient.getJobLogs(userId, jobName, sinceTime)
        if (result.isNotOk()) {
            return DispatchJobLogResp(
                log = result.data,
                errorMsg = result.message
            )
        }
        return DispatchJobLogResp(log = result.data)
    }
}
