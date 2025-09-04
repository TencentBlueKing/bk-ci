package com.tencent.devops.dispatch.devcloud.listener

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.ContainerBuildStatus
import com.tencent.devops.dispatch.devcloud.service.DcContainerShutdownHandler
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudBuildRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ContainerStatusUpdateJob @Autowired constructor(
    private val bkTag: BkTag,
    private val dslContext: DSLContext,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val redisOperation: RedisOperation,
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    private val dcContainerShutdownHandler: DcContainerShutdownHandler
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerStatusUpdateJob::class.java)
        private const val jobLockKey = "dispatch_devcloud_cron_container_clear_job"
        private const val debugLockKey = "dispatch_devcloud_cron_container_debug_clear_job"
    }

    @Scheduled(cron = "0 0 2 * * ?")
    fun run() {
        logger.info("ContainerStatusUpdateJob start")
        val redisLock = getContainerClearJobLock()
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("ContainerStatusUpdateJob start")
                executeTask()
            }
        } catch (e: Throwable) {
            logger.error("ContainerStatusUpdateJob exception", e)
        } finally {
            redisLock.unlock()
        }
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    fun runDebugRecycling() {
        val redisLock = getContainerDebugLock()
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("ContainerStatusUpdateJob runDebugRecycling start")
                clearDebugContainer()
            }
        } catch (e: Throwable) {
            logger.error("ContainerStatusUpdateJob runDebugRecycling exception", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun getContainerClearJobLock(): RedisLock {
        return if (bkTag.getLocalTag().contains("auto")) {
            RedisLock(redisOperation, jobLockKey + "auto", 3600L)
        } else {
            RedisLock(redisOperation, jobLockKey, 3600L)
        }
    }

    private fun getContainerDebugLock(): RedisLock {
        return if (bkTag.getLocalTag().contains("auto")) {
            RedisLock(redisOperation, debugLockKey + "auto", 3600L)
        } else {
            RedisLock(redisOperation, debugLockKey, 3600L)
        }
    }

    private fun executeTask() {
        // 超过七天还在用的容器，先查状态，如果是stop，则刷新db，否则先stop再刷db
        val timeOutBusyContainerList = devCloudBuildDao.getTimeOutBusyContainer(dslContext)
        logger.info("Start clearTimeOutBusyContainer: ${System.currentTimeMillis()}")

        if (timeOutBusyContainerList.isNotEmpty) {
            timeOutBusyContainerList.forEach {
                clearTimeOutBusyContainer(it)
            }
        }

        clearNoUseIdleContainer()
    }

    private fun clearTimeOutBusyContainer(it: TDevcloudBuildRecord) {
        logger.info("clearTimeOutBusyContainer PipelineId: ${it.pipelineId}|vmSeqId:${it.vmSeqId}|" +
                        "poolNo:${it.poolNo}|ContainerName: ${it.containerName}")
        try {
            // 重新check一下DB状态
            val devcloudBuild = devCloudBuildDao.get(dslContext, it.pipelineId, it.vmSeqId, it.poolNo)
            if (devcloudBuild == null ||
                devcloudBuild.status == 0 ||
                (devcloudBuild.updateTime.plusDays(7) > LocalDateTime.now())
            ) {
                return
            }

            val statusResponse = dispatchDevCloudClient.getContainerStatus(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                buildId = "",
                vmSeqId = it.vmSeqId,
                userId = it.userId,
                name = it.containerName
            )
            val actionCode = statusResponse.optInt("actionCode")
            if (actionCode != 200) {
                return
            }

            when (val status = statusResponse.optString("data")) {
                "stopped", "stop", "deleted" -> {
                    logger.info("Update status to idle, containerName: ${it.containerName}")
                    devCloudBuildDao.updateStatus(
                        dslContext = dslContext,
                        pipelineId = it.pipelineId,
                        vmSeqId = it.vmSeqId,
                        poolNo = it.poolNo,
                        status = ContainerBuildStatus.IDLE.status
                    )
                }
                "running" -> {
                    dcContainerShutdownHandler.forceStopContainer(devcloudBuild)
                }
                else -> {
                    // 异常或其他状态的删除
                    logger.info("Status exception, containerName: ${it.containerName}, status: $status")
                    devCloudBuildDao.delete(dslContext, it.pipelineId, it.vmSeqId, it.poolNo)
                    dispatchDevCloudClient.operateContainer(
                        projectId = it.projectId,
                        pipelineId = it.pipelineId,
                        buildId = "",
                        vmSeqId = it.vmSeqId,
                        userId = it.userId,
                        name = it.containerName,
                        action = Action.STOP
                    )
                }
            }
        } catch (e: Throwable) {
            logger.warn(
                "clearTimeOutBusyContainer exception, PipelineId: ${it.pipelineId}|" +
                    "vmSeqId:${it.vmSeqId}|poolNo:${it.poolNo}|ContainerName: ${it.containerName}", e
            )
            devCloudBuildDao.updateStatus(
                dslContext = dslContext,
                pipelineId = it.pipelineId,
                vmSeqId = it.vmSeqId,
                poolNo = it.poolNo,
                status = ContainerBuildStatus.IDLE.status
            )
        }
    }

    private fun clearNoUseIdleContainer() {
        // 超过7天空闲的容器，直接删了
        val noUseIdleContainerList = devCloudBuildDao.getNoUseIdleContainer(dslContext)
        if (noUseIdleContainerList.isNotEmpty) {
            noUseIdleContainerList.forEach {
                devCloudBuildDao.delete(dslContext, it.pipelineId, it.vmSeqId, it.poolNo)
                try {
                    dispatchDevCloudClient.operateContainer(
                        projectId = it.projectId,
                        pipelineId = it.pipelineId,
                        buildId = "",
                        vmSeqId = it.vmSeqId,
                        userId = it.userId,
                        name = it.containerName,
                        action = Action.DELETE
                    )
                } catch (e: Throwable) {
                    logger.error("clearNoUseIdleContainer exception:", e)
                }
            }
        }
    }

    private fun clearDebugContainer() {
        // 超过一小时处于debug状态并且空闲的容器，先查状态，如果是stop，则刷新db debug状态，否则先stop再刷db
        // 开启事务
        val timeoutBusyDebugContainerList = devCloudBuildDao.getTimeoutBusyDebugContainer(dslContext)
        timeoutBusyDebugContainerList.forEach {
            logger.info(
                "Start clearDebugContainer pipelineId: ${it.pipelineId}|vmSeqId:${it.vmSeqId}|" +
                        "poolNo:${it.poolNo}|ContainerName: ${it.containerName}"
            )
            try {
                val statusResponse = dispatchDevCloudClient.getContainerStatus(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    buildId = "",
                    vmSeqId = it.vmSeqId,
                    userId = it.userId,
                    name = it.containerName
                )
                val actionCode = statusResponse.optInt("actionCode")
                if (actionCode != 200) {
                    return@forEach
                }
                when (val status = statusResponse.optString("data")) {
                    "stopped", "stop" -> {
                        logger.info("Update debug status to false, containerName: ${it.containerName}")
                        devCloudBuildDao.updateDebugStatus(
                            dslContext,
                            it.pipelineId,
                            it.vmSeqId,
                            it.containerName,
                            false
                        )
                    }

                    "running" -> {
                        dcContainerShutdownHandler.forceStopContainer(it)
                    }

                    else -> {
                        // 异常或其他状态的只更新debug状态，不做删除，因为devcloud对于异常状态构建机会自愈
                        logger.info("Status exception, containerName: ${it.containerName}, status: $status")
                        devCloudBuildDao.updateDebugStatus(
                            dslContext,
                            it.pipelineId,
                            it.vmSeqId,
                            it.containerName,
                            false
                        )
                    }
                }
            } catch (e: Throwable) {
                logger.error(
                    "clearDebugContainer exception pipelineId: ${it.pipelineId}|vmSeqId:${it.vmSeqId}|" +
                            "poolNo:${it.poolNo}|ContainerName: ${it.containerName}.", e
                )
                devCloudBuildDao.updateDebugStatus(
                    dslContext,
                    it.pipelineId,
                    it.vmSeqId,
                    it.containerName,
                    false
                )
            }
        }
    }
}
