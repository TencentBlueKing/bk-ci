package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceContainerDao
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.ContainerBuildStatus
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
        val container = dcPersistenceContainerDao.getByPersistenceAgentId(dslContext, persistenceAgentId)
        if (container == null) {
            logger.warn("Container $persistenceAgentId is null.")
            return null
        }

        // 当前容器非running状态，主动清理
        if (container.containerStatus != PersistenceContainerStatus.RUNNING.status) {
            deletePersistenceContainer(
                userId = container.userId,
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                persistenceAgentId = persistenceAgentId,
                vmSeqId = container.vmSeqId,
                containerName = container.containerName
            )

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
        val persistenceContainerRecord = dcPersistenceContainerDao.getByPersistenceAgentId(
            dslContext,
            persistenceAgentId
        )
        if (persistenceContainerRecord == null) {
            logger.warn("Container $persistenceAgentId is null.")
            return
        }

        // 重置persistenceContainer状态
        dcPersistenceContainerDao.updateBuildStatus(
            dslContext,
            persistenceAgentId,
            ContainerBuildStatus.IDLE.status
        )

        // 重新设置persistence build状态
        val buildRecord = dcPersistenceBuildDao.getPersistenceBuildInfo(
            dslContext = dslContext,
            buildId = buildInfo.buildId,
            vmSeqId = buildInfo.vmSeqId,
            executeCount = buildInfo.executeCount
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
            logger.error(
                "SystemErrorLogMonitor|onContainerFailure|${buildInfo.buildId}|" +
                    "error=${buildInfo.error}"
            )
        }
    }

    fun destroyPersistenceContainer(userId: String, destroyContainerReq: DestroyContainerReq): Result<Boolean> {
        logger.info("$userId destroy container $destroyContainerReq")
        if (!destroyContainerReq.persistenceAgentId.isNullOrBlank()) {
            val containerName = dcPersistenceContainerDao.getByPersistenceAgentId(
                dslContext,
                destroyContainerReq.persistenceAgentId!!
            )?.containerName ?: ""

            deletePersistenceContainer(
                userId = userId,
                projectId = destroyContainerReq.projectId,
                pipelineId = destroyContainerReq.pipelineId,
                persistenceAgentId = destroyContainerReq.persistenceAgentId!!,
                vmSeqId = destroyContainerReq.vmSeqId,
                containerName = containerName
            )

            return Result(true)
        }

        val buildRecords = dcPersistenceContainerDao.getPersistenceContainer(
            dslContext,
            destroyContainerReq.pipelineId,
            destroyContainerReq.vmSeqId,
            destroyContainerReq.poolNo
        )

        buildRecords.forEach {
            deletePersistenceContainer(
                userId = userId,
                projectId = destroyContainerReq.projectId,
                pipelineId = destroyContainerReq.pipelineId,
                persistenceAgentId = it.persistenceAgentId,
                vmSeqId = destroyContainerReq.vmSeqId,
                containerName = it.containerName
            )
        }

        return Result(true)
    }

    private fun deletePersistenceContainer(
        userId: String,
        projectId: String,
        pipelineId: String,
        persistenceAgentId: String,
        vmSeqId: String,
        containerName: String
    ) {
        val buildLogKey = "$userId|$projectId|$pipelineId|$vmSeqId|$persistenceAgentId"

        dcPersistenceContainerDao.updateContainerStatus(
            dslContext,
            persistenceAgentId,
            PersistenceContainerStatus.DELETED.status
        )

        try {
            logger.info("$buildLogKey delete container:$containerName")
            val taskId = dispatchDevCloudClient.operateContainer(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = "",
                vmSeqId = vmSeqId,
                userId = userId,
                name = containerName,
                action = Action.DELETE
            )
            val opResult = dispatchDevCloudClient.waitTaskFinish(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId
            )
            if (opResult.first == TaskStatus.SUCCEEDED) {
                logger.info("$buildLogKey delete $containerName success.")
            } else {
                logger.info("$buildLogKey delete $containerName failed, msg: ${opResult.second}")
            }
        } catch (e: Exception) {
            logger.error(
                "$buildLogKey delete $containerName failed.",
                e
            )
        }
    }
}
