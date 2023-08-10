package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.pojo.SimpleResult
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceContainerDao
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
    private val dcPersistenceBuildDao: DcPersistenceBuildDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PersistenceBuildService::class.java)
    }

    fun startBuild(projectId: String, containerName: String): PersistenceBuildInfo? {
        // 检查containerName当前状态
        val container = dcPersistenceContainerDao.getContainerStatus(dslContext, containerName)
        if (container == null || container.containerStatus != PersistenceContainerStatus.RUNNING.status) {
            logger.warn("Container $containerName is null or status not running.")
            return null
        }

        val lock = PersistenceContainerLock(redisOperation, containerName)
        try {
            if (lock.tryLock()) {
                val buildRecord = dcPersistenceBuildDao.fetchOneQueueBuild(dslContext, containerName) ?: run {
                    logger.warn("No build for $containerName in queue.")
                    return null
                }

                dcPersistenceBuildDao.updateStatus(
                    dslContext = dslContext,
                    id = buildRecord.id,
                    status = PersistenceBuildStatus.RUNNING.status
                )

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

    fun workerBuildFinish(projectId: String, containerName: String, buildInfo: PersistenceBuildWithStatus) {
        val container = dcPersistenceContainerDao.getContainerStatus(dslContext, containerName)
        if (container == null || container.containerStatus != PersistenceContainerStatus.RUNNING.status) {
            logger.warn("Container $containerName is null or status not running.")
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

        client.get(ServiceBuildResource::class).workerBuildFinish(
            projectId = buildInfo.buildId,
            pipelineId = buildInfo.pipelineId ?: "",
            buildId = buildInfo.buildId,
            vmSeqId = buildInfo.vmSeqId,
            nodeHashId = containerName,
            simpleResult = SimpleResult(
                success = buildInfo.success,
                message = buildInfo.message,
                error = buildInfo.error
            )
        )
    }
}
