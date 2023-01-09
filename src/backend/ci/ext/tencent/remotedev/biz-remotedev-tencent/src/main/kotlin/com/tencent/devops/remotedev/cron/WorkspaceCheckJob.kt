package com.tencent.devops.remotedev.cron

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WorkspaceCheckJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisHeartBeat: RedisHeartBeat,
    private val redisOperation: RedisOperation,
    private val remoteDevDispatcher: RemoteDevDispatcher,
    private val workspaceService: WorkspaceService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCheckJob::class.java)
        private const val jobLockKey = "dispatch_devcloud_cron_workspace_clear_job"
    }

    /**
     * 每5min检测一次 30min内没有心跳上报的工作空间，主动stop
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun stopInactiveWorkspace() {
        logger.info("=========>> Stop inactive workspace <<=========")
        val redisLock = RedisLock(redisOperation, jobLockKey, 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Stop inactive workspace get lock.")
                val sleepWorkspaceList = redisHeartBeat.getSleepWorkspaceHeartbeats()
                sleepWorkspaceList.parallelStream().forEach {
                    workspaceService.heartBeatStopWS(it)
                    redisHeartBeat.deleteWorkspaceHeartbeat("admin", it)
                }
            }
        } catch (e: Throwable) {
            logger.error("Stop inactive workspace failed", e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 每天凌晨2点触发，检测空闲超过七天的工作空间并销毁
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun clearIdleWorkspace() {
        logger.info("=========>> Clear idle workspace <<=========")
        val redisLock = RedisLock(redisOperation, jobLockKey, 3600L)
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
}
