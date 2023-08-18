package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.SimpleResult
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceContainerDao
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.DestroyContainerReq
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildInfo
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildWithStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceContainerStatus
import com.tencent.devops.dispatch.devcloud.utils.PersistenceContainerLock
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PersistenceBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dcPersistenceContainerDao: DcPersistenceContainerDao,
    private val dcPersistenceBuildDao: DcPersistenceBuildDao,
    private val dispatchDevCloudClient: DispatchDevCloudClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PersistenceBuildService::class.java)
    }

    fun startBuild(projectId: String, persistenceAgentId: String): PersistenceBuildInfo? {
        // 检查containerName当前状态
        val container = dcPersistenceContainerDao.get(dslContext, persistenceAgentId)
        if (container == null) {
            logger.warn("Container $persistenceAgentId is null.")
            return null
        }

        val lock = PersistenceContainerLock(redisOperation, persistenceAgentId)
        try {
            if (lock.tryLock()) {
                val buildRecord = dcPersistenceBuildDao.fetchOneQueueBuild(dslContext, persistenceAgentId) ?: run {
                    logger.warn("No build for $persistenceAgentId in queue.")
                    return null
                }

                dcPersistenceBuildDao.updateStatus(
                    dslContext = dslContext,
                    id = buildRecord.id,
                    status = PersistenceBuildStatus.RUNNING.status
                )

                logger.info("Container $persistenceAgentId start build $buildRecord")

                return PersistenceBuildInfo(
                    projectId = buildRecord.projectId,
                    pipelineId = buildRecord.pipelineId,
                    buildId = buildRecord.buildId,
                    vmSeqId = buildRecord.vmSeqId,
                    workspace = "",
                    agentId = buildRecord.agentId,
                    secretKey = buildRecord.secretKey,
                    executeCount = buildRecord.executeCount,
                    containerHashId = buildRecord.containerHashId
                )
            }

            return null
        } finally {
            lock.unlock()
        }
    }

    fun workerBuildFinish(projectId: String, persistenceAgentId: String, buildInfo: PersistenceBuildWithStatus) {
        logger.info("$projectId $persistenceAgentId workerBuildFinish $buildInfo")
        val container = dcPersistenceContainerDao.get(dslContext, persistenceAgentId)
        if (container == null) {
            logger.warn("Container $persistenceAgentId is null.")
        }

        // 重新设置container build状态
        val buildRecord = dcPersistenceBuildDao.getPersistenceBuildInfo(
            dslContext = dslContext,
            buildId = buildInfo.buildId,
            vmSeqId = buildInfo.vmSeqId
        )
        if (buildRecord != null && (
                buildRecord.status != PersistenceBuildStatus.DONE.status ||
                    buildRecord.status != PersistenceBuildStatus.FAILURE.status)
            ) {
            dcPersistenceBuildDao.updateStatus(
                dslContext = dslContext,
                id = buildRecord.id,
                status = if (buildInfo.success) {
                    PersistenceBuildStatus.DONE.status
                } else {
                    PersistenceBuildStatus.FAILURE.status
                }
            )
        }

        try {
            client.get(ServiceBuildResource::class).setVMStatus(
                projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId ?: "",
                buildId = buildInfo.buildId,
                vmSeqId = buildInfo.vmSeqId,
                status = if (buildInfo.success) {
                    BuildStatus.SUCCEED
                } else {
                    BuildStatus.FAILED
                },
                errorType = ErrorType.SYSTEM,
                errorCode = buildInfo.error?.errorCode ?: 0,
                errorMsg = buildInfo.error?.errorMessage ?: ""
            )
        } catch (ignore: ClientException) {
            logger.error("SystemErrorLogMonitor|onContainerFailure|${buildInfo.buildId}|" +
                             "error=${buildInfo.error}")
        }
    }

    fun destroyContainer(userId: String, destroyContainerReq: DestroyContainerReq): Result<Boolean> {
        logger.info("$userId destroy container $destroyContainerReq")
        val containerName = if (destroyContainerReq.containerName == null) {
            val buildRecord = dcPersistenceContainerDao.get(
                dslContext,
                destroyContainerReq.pipelineId,
                destroyContainerReq.vmSeqId
            )
            buildRecord?.containerName ?: ""
        } else {
            destroyContainerReq.containerName
        }

        if (containerName.isNullOrBlank()) {
            logger.warn("$userId destroy containerName is null.")
            return Result(
                status = 500,
                message = "ContainerName is null",
                data = false
            )
        }

        val buildLogKey = "$userId|${destroyContainerReq.projectId}|${destroyContainerReq.pipelineId}" +
            "|${destroyContainerReq.vmSeqId}"

        try {
            logger.info("$buildLogKey stop dev cloud container, containerName:$containerName")
            val taskId = dispatchDevCloudClient.operateContainer(
                projectId = destroyContainerReq.projectId,
                pipelineId = destroyContainerReq.pipelineId,
                buildId = "",
                vmSeqId = destroyContainerReq.vmSeqId ?: "",
                userId = userId,
                name = containerName,
                action = Action.STOP
            )
            val opResult = dispatchDevCloudClient.waitTaskFinish(
                userId = userId,
                projectId = destroyContainerReq.projectId,
                pipelineId = destroyContainerReq.pipelineId,
                taskId = taskId
            )
            if (opResult.first == TaskStatus.SUCCEEDED) {
                logger.info("$buildLogKey stop dev cloud vm success.")
            } else {
                logger.info("$buildLogKey stop dev cloud vm failed, msg: ${opResult.second}")
            }
        } catch (e: Exception) {
            logger.error(
                "$buildLogKey stop dev cloud vm failed. containerName: $containerName",
                e
            )
        }

        return Result(true)
    }
}
