package com.tencent.devops.dispatch.codecc.listener

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.codecc.client.CodeccDevCloudClient
import com.tencent.devops.dispatch.codecc.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.codecc.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ContainerStatus
import com.tencent.devops.dispatch.codecc.pojo.devcloud.TaskStatus
import com.tencent.devops.dispatch.codecc.pojo.devcloud.Action
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ContainerStatusUpdateJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val codeccDevCloudClient: CodeccDevCloudClient,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerStatusUpdateJob::class.java)
        private const val jobLockKey = "dispatch_codecc_cron_container_clear_job"
    }

    @Scheduled(cron = "0 0 2 * * ?")
    fun run() {
        logger.info("ContainerStatusUpdateJob start")
        val redisLock = RedisLock(redisOperation,
                                  jobLockKey, 3600L)
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

    private fun executeTask() {
        clearTimeOutBusyContainer()
        clearNoUseIdleContainer()
    }

    private fun clearTimeOutBusyContainer() {
        try {
            // 超过两天还在用的容器，先查状态，如果是stop，则刷数db，否则先stop再刷db
            val timeOutBusyContainerList = devCloudBuildDao.getTimeOutBusyContainer(dslContext)
            if (timeOutBusyContainerList.isNotEmpty) {
                timeOutBusyContainerList.forEach {
                    logger.info("PipelineId: ${it.pipelineId}|vmSeqId:${it.vmSeqId}|poolNo:${it.poolNo}|ContainerName: ${it.containerName}")
                    val buildHistory = devCloudBuildHisDao.getLatestBuildHistory(dslContext, it.pipelineId, it.vmSeqId)
                    val statusResponse = codeccDevCloudClient.getContainerStatus(buildHistory!!.codeccTaskId, it.userId, it.containerName)
                    val actionCode = statusResponse.optInt("actionCode")
                    if (actionCode == 200) {
                        when (val status = statusResponse.optString("data")) {
                            "stopped", "stop" -> {
                                logger.info("Update status to idle, containerName: ${it.containerName}")
                                devCloudBuildDao.updateStatus(dslContext, it.pipelineId, it.vmSeqId, it.poolNo, ContainerStatus.IDLE.status)
                            }
                            "running" -> {
                                logger.info("Container is running, stop it, containerName:${it.containerName}")
                                val taskId = codeccDevCloudClient.operateContainer(buildHistory!!.codeccTaskId, it.userId, it.containerName, Action.STOP)
                                val opResult = codeccDevCloudClient.waitTaskFinish(buildHistory!!.codeccTaskId, it.userId, taskId)
                                if (opResult.first == TaskStatus.SUCCEEDED) {
                                    logger.info("stop dev cloud vm success. then update status to idle")
                                    devCloudBuildDao.updateStatus(dslContext, it.pipelineId, it.vmSeqId, it.poolNo, ContainerStatus.IDLE.status)
                                } else {
                                    // 停不掉？尝试删除
                                    logger.info("stop dev cloud vm failed, msg: ${opResult.second}")
                                    logger.info("stop dev cloud vm failed, try to delete it, containerName:${it.containerName}")
                                    codeccDevCloudClient.operateContainer(buildHistory!!.codeccTaskId, it.userId, it.containerName, Action.DELETE)
                                    devCloudBuildDao.delete(dslContext, it.pipelineId, it.vmSeqId, it.poolNo)
                                }
                            }
                            else -> {
                                // 异常或其他状态的删除
                                logger.info("Status exception, containerName: ${it.containerName}, status: $status")
                                codeccDevCloudClient.operateContainer(buildHistory!!.codeccTaskId, it.userId, it.containerName, Action.DELETE)
                                devCloudBuildDao.delete(dslContext, it.pipelineId, it.vmSeqId, it.poolNo)
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.error("clearTimeOutBusyContainer exception:", e)
        }
    }

    private fun clearNoUseIdleContainer() {
        // 超过30天空闲的容器，直接删了
        val noUseIdleContainerList = devCloudBuildDao.getNoUseIdleContainer(dslContext)
        if (noUseIdleContainerList.isNotEmpty) {
            noUseIdleContainerList.forEach {
                val buildHistory = devCloudBuildHisDao.getLatestBuildHistory(dslContext, it.pipelineId, it.vmSeqId)
                devCloudBuildDao.delete(dslContext, it.pipelineId, it.vmSeqId, it.poolNo)
                try {
                    codeccDevCloudClient.operateContainer(buildHistory!!.codeccTaskId, it.userId, it.containerName, Action.DELETE)
                } catch (e: Throwable) {
                    logger.error("clearNoUseIdleContainer exception:", e)
                }
            }
        }
    }
}
