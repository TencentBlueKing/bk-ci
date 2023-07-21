package com.tencent.devops.remotedev.cron

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WorkspaceCheckJob @Autowired constructor(
    private val redisHeartBeat: RedisHeartBeat,
    private val redisOperation: RedisOperation,
    private val workspaceService: WorkspaceService,
    private val bkTag: BkTag,
    private val sleepControl: SleepControl,
    private val workspaceCommon: WorkspaceCommon,
    private val deleteControl: DeleteControl
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCheckJob::class.java)
        // 根据心跳操作工作空间
        private const val stopJobLockKeyH = "dispatch_devcloud_cron_workspace_clear_job_heartbeats"
        // 根据用户使用时长操作工作空间
        private const val stopJobLockKeyD = "dispatch_devcloud_cron_workspace_clear_job_duration"
        private const val deleteJobLockKey = "dispatch_devcloud_cron_workspace_delete_job"
        private const val nofityJobLockKey = "dispatch_devcloud_cron_workspace_nofity_job"
        private const val billJobLockKey = "dispatch_devcloud_cron_workspace_init_bill"
    }

    /**
     * 每5min检测一次 30min内没有心跳上报的工作空间，主动stop
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun stopInactiveWorkspace() {
        logger.info("=========>> Stop inactive workspace <<=========")
        checkInactiveWorkspace()
        checkUnavailableWorkspace()
    }

    private fun checkUnavailableWorkspace() {
        val redisLock = RedisLock(redisOperation, stopJobLockKeyD + bkTag.getLocalTag(), 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Stop Unavailable workspace get lock.")
                val sleepWorkspaceList = workspaceService.getUnavailableWorkspace()
                sleepWorkspaceList.parallelStream().forEach { workspaceName ->
                    MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
                    logger.info(
                        "workspace $workspaceName usage time exceeds limit, ready to sleep"
                    )
                    kotlin.runCatching {
                        sleepControl.heartBeatStopWS(workspaceName, OpHistoryCopyWriting.EXPERIENCE_TIMEOUT_SLEEP)
                    }.onFailure {
                        logger.warn("heart beat stop ws $workspaceName fail, ${it.message}")
                    }
                }
            }
        } catch (e: Throwable) {
            logger.error("Stop inactive workspace failed", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun checkInactiveWorkspace() {
        val redisLock = RedisLock(redisOperation, stopJobLockKeyH + bkTag.getLocalTag(), 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Stop inactive workspace get lock.")
                if (redisHeartBeat.autoHeartbeat()) return
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
                        sleepControl.heartBeatStopWS(workspaceName, OpHistoryCopyWriting.TIMEOUT_SLEEP)
                    }.onFailure {
                        logger.warn("heart beat stop ws $workspaceName fail, ${it.message}")
                        // 针对已经休眠或销毁的容器，删除上报心跳记录。
                        if (it is ErrorCodeException &&
                            (
                                it.errorCode == ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode ||
                                    it.errorCode == ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode
                                )
                        ) {
                            redisHeartBeat.deleteWorkspaceHeartbeat(ADMIN_NAME, workspaceName)
                        }
                    }
                }
                workspaceCommon.fixUnexpectedWorkspace()
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
        val redisLock = RedisLock(redisOperation, deleteJobLockKey + bkTag.getLocalTag(), 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Clear idle workspace get lock.")
                deleteControl.deleteInactivityWorkspace()
            }
        } catch (e: Throwable) {
            logger.error("Clear idle workspace failed", e)
        } finally {
            redisLock.unlock()
        }
    }
    /**
     * 每天10点触发，检测即将空闲超过14天的工作空间并做邮件推送
     */
    @Scheduled(cron = "0 0 10 * * ?")
    fun sendIdleWorkspaceNotify() {
        logger.info("=========>> send idle workspace notify <<=========")
        if (!SpringContextUtil.getBean(Profile::class.java).isProd()) {
            return
        }
        val redisLock = RedisLock(redisOperation, nofityJobLockKey, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("send idle workspace notify get lock.")
                workspaceService.sendInactivityWorkspaceNotify()
            }
        } catch (e: Throwable) {
            logger.error("send idle workspace notify failed", e)
        }
    }
    /**
     * 每月1号4点执行任务触发，对用户收费时间进行重置
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    fun initBilling() {
        logger.info("=========>> time to initBilling <<=========")
        val redisLock = RedisLock(redisOperation, billJobLockKey, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("ready to init bill")
                workspaceService.initBilling()
            }
        } catch (e: Throwable) {
            logger.error("failed to init bill", e)
        }
    }
}
