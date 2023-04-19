package com.tencent.devops.remotedev.cron

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WorkspaceCheckJob @Autowired constructor(
    private val redisHeartBeat: RedisHeartBeat,
    private val redisOperation: RedisOperation,
    private val workspaceService: WorkspaceService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCheckJob::class.java)
        private const val stopJobLockKey = "dispatch_devcloud_cron_workspace_clear_job"
        private const val deleteJobLockKey = "dispatch_devcloud_cron_workspace_delete_job"
        private const val billJobLockKey = "dispatch_devcloud_cron_workspace_init_bill"
    }

    /**
     * 每5min检测一次 30min内没有心跳上报的工作空间，主动stop
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun stopInactiveWorkspace() {
        logger.info("=========>> Stop inactive workspace <<=========")
        val redisLock = RedisLock(redisOperation, stopJobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Stop inactive workspace get lock.")
                val sleepWorkspaceList = redisHeartBeat.getSleepWorkspaceHeartbeats()
                sleepWorkspaceList.parallelStream().forEach { (workspaceName, time) ->
                    MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
                    logger.info(
                        "workspace $workspaceName last active is ${
                            DateTimeUtil.formatMilliTime(
                                time.toLong(),
                                DateTimeUtil.YYYY_MM_DD_HH_MM_SS
                            )
                        } ready to sleep"
                    )
                    kotlin.runCatching {
                        workspaceService.heartBeatStopWS(workspaceName)
                    }.onFailure { logger.warn("heart beat stop ws $workspaceName fail, ${it.message}") }
                }
                workspaceService.fixUnexpectedWorkspace()
            }
        } catch (e: Throwable) {
            logger.error("Stop inactive workspace failed", e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每天凌晨2点触发，检测空闲超过14天的工作空间并销毁
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun clearIdleWorkspace() {
        logger.info("=========>> Clear idle workspace <<=========")
        val redisLock = RedisLock(redisOperation, deleteJobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Clear idle workspace get lock.")
                workspaceService.deleteInactivityWorkspace()
            }
        } catch (e: Throwable) {
            logger.error("Clear idle workspace failed", e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每月1号4点执行任务触发，对用户收费时间进行重置
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    fun initBilling() {
        logger.info("=========>> time to initBilling <<=========")
        val redisLock = RedisLock(redisOperation, billJobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("ready to init bill")
                workspaceService.initBilling()
            }
        } catch (e: Throwable) {
            logger.error("failed to init bill", e)
        } finally {
            redisLock.unlock()
        }
    }
}
