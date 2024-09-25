package com.tencent.devops.remotedev.cron

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WorkspaceCheckJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val workspaceService: WorkspaceService,
    private val bkTag: BkTag,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCheckJob::class.java)

        // 根据心跳操作工作空间
        private const val stopJobLockKeyH = "dispatch_devcloud_cron_workspace_clear_job_heartbeats"
        private const val syncJobLockKey = "remotedev_cron_sync_start_resource_job"
        private const val backupCgsDataLockKey = "remotedev_cron_backup_csg_data_job"
    }

    /**
     * 每5min检测一次 30min内没有心跳上报的工作空间，主动stop
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun stopInactiveWorkspace() {
        logger.info("=========>> Stop inactive workspace <<=========")
        // 定时修复异常状态
        checkWorkspaceStatus()
    }

    private fun checkWorkspaceStatus() {
        val redisLock = RedisLock(redisOperation, stopJobLockKeyH + bkTag.getLocalTag(), 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                workspaceCommon.fixUnexpectedWorkspace()
            }
        } catch (e: Throwable) {
            logger.error("Stop inactive workspace failed", e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 定时同步更新START云桌面资源池 5min一次
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun syncStartCloudResourceList() {
        logger.info("=========>> start to sync START resource list <<=========")
        if (!SpringContextUtil.getBean(Profile::class.java).isProd()) {
            return
        }
        val redisLock = RedisLock(redisOperation, syncJobLockKey, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("sync START resource list get lock.")
                windowsResourceConfigService.syncStartCloudResourceList()
            }
        } catch (e: Throwable) {
            logger.error("sync START resource list failed", e)
        }
    }

    /**
     * 23:50 定时统计云桌面数据快照
     */
    @Scheduled(cron = "0 50 23 * * ?")
    fun backupDailyCgsData() {
        logger.info("=========>> start to back up cgs data <<=========")
        if (!SpringContextUtil.getBean(Profile::class.java).isProd()) {
            return
        }
        val redisLock = RedisLock(redisOperation, backupCgsDataLockKey, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("sync backup cgs data get lock.")
                workspaceCommon.backupDailyCsgData()
            }
        } catch (e: Throwable) {
            logger.error("sync START resource list failed", e)
        }
    }

    /**
     * 每 10 分钟检测一次过期的工作空间分享
     */
    @Scheduled(cron = "0 */10 * ? * *")
    fun checkAndUnshared() {
        workspaceService.checkAndUnshared()
    }
}
