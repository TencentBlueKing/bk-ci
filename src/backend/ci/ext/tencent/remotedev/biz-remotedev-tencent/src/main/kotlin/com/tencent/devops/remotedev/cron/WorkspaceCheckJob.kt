package com.tencent.devops.remotedev.cron

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WorkspaceCheckJob @Autowired constructor(
    private val redisHeartBeat: RedisHeartBeat,
    private val redisOperation: RedisOperation,
    private val workspaceService: WorkspaceService,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val bkTag: BkTag,
    private val sleepControl: SleepControl,
    private val workspaceCommon: WorkspaceCommon,
    private val deleteControl: DeleteControl,
    private val holidayHelper: HolidayHelper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCheckJob::class.java)

        // 根据心跳操作工作空间
        private const val stopJobLockKeyH = "dispatch_devcloud_cron_workspace_clear_job_heartbeats"

        // 根据用户使用时长操作工作空间
        private const val stopJobLockKeyD = "dispatch_devcloud_cron_workspace_clear_job_duration"
        private const val deleteJobLockKey = "dispatch_devcloud_cron_workspace_delete_job"
        private const val nofityJobLockKey = "dispatch_devcloud_cron_workspace_nofity_job"
        private const val winJobLockKey = "dispatch_devcloud_cron_workspace_win_job"
        private const val billJobLockKey = "dispatch_devcloud_cron_workspace_init_bill"
        private const val syncJobLockKey = "remotedev_cron_sync_start_resource_job"
        private const val computeAllUserWinUsageTime = "dispatch_devcloud_cron_workspace_computeAllUserWinUsageTime"
        private const val notifyWinBeforeSleep = "dispatch_devcloud_cron_notify_win_before_sleep"
        private const val backupCgsDataLockKey = "remotedev_cron_backup_csg_data_job"
    }

    @Value("\${remoteDev.autoDeletePipeline:}")
    val autoDeletePipeline = ""

    /**
     * 每5min检测一次 30min内没有心跳上报的工作空间，主动stop
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun stopInactiveWorkspace() {
        logger.info("=========>> Stop inactive workspace <<=========")
        // 无心跳工作空间休眠
        checkLinuxInactiveWorkspace()
        /*暂时取消个人云桌面控制*/
        // 计算用户 win-gpu 可用时长
//        computeAllUserWinUsageTime()
        // win-gpu 无可用时长休眠
//        checkUnavailableWorkspace()
        // win-gpu 提醒
//        notifyWinBeforeSleep()
    }

    private fun notifyWinBeforeSleep() {
        val redisLock = RedisLock(redisOperation, notifyWinBeforeSleep, 60L)
        val lockSuccess = redisLock.tryLock()
        if (lockSuccess) {
            kotlin.runCatching { workspaceService.notifyWinBeforeSleep() }
                .onFailure { logger.warn("computeAllUserWinUsageTime fail", it) }
        }
    }

    private fun computeAllUserWinUsageTime() {
        val redisLock = RedisLock(redisOperation, computeAllUserWinUsageTime, 60L)
        val lockSuccess = redisLock.tryLock()
        if (lockSuccess) {
            kotlin.runCatching { remoteDevSettingService.computeWinUsageTime() }
                .onFailure { logger.warn("computeAllUserWinUsageTime fail", it) }
        }
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

    private fun checkLinuxInactiveWorkspace() {
        val redisLock = RedisLock(redisOperation, stopJobLockKeyH + bkTag.getLocalTag(), 3600L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Stop inactive workspace get lock.")
                if (redisHeartBeat.autoHeartbeat()) {
                    workspaceCommon.fixUnexpectedWorkspace()
                    return
                }
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
                // 保留在此：处理休眠失败的情况
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
                deleteControl.deleteLinuxInactivityWorkspace()
                /*暂时去掉个人win的控制*/
//                deleteControl.deleteWinInactivityWorkspace()
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
                workspaceService.sendLinuxInactivityWorkspaceNotify()
                /*暂时去掉个人win的控制*/
//                workspaceService.sendWinInactivityWorkspaceNotify()
            }
        } catch (e: Throwable) {
            logger.error("send idle workspace notify failed", e)
        }
    }

    /**
     * 每天10点触发，执行云桌面专项空闲工作空间检测
     */
    @Scheduled(cron = "0 0 10 * * ?")
    fun projectWinJob() {
        logger.info("=========>> projectWinJob <<=========")

        val redisLock = RedisLock(redisOperation, winJobLockKey, 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (!holidayHelper.isWorkingDay(LocalDateTime.now())) {
                logger.warn("not working day, so ignore.")
                return
            }
            if (lockSuccess) {
                val readyDeleteWorkspace = mutableListOf<String>()
                // 云桌面处于待分配超过3天的自动回收，并邮件提醒
                kotlin.runCatching {
                    deleteControl.autoDeleteWhenNotAssign(false, readyDeleteWorkspace)
                }.onFailure {
                    logger.warn("autoDeleteWhenNotAssign fail ${it.message}", it)
                }

                // 关机超过14天时自动销毁
                kotlin.runCatching {
                    deleteControl.autoDeleteWhenSleep14Day(false, readyDeleteWorkspace)
                    if (readyDeleteWorkspace.isNotEmpty()) {
                        logger.info("read to notify system manager|$readyDeleteWorkspace")
                        OkhttpUtils.doPost(
                            autoDeletePipeline,
                            JsonUtil.toJson(
                                mapOf(
                                    "infos" to readyDeleteWorkspace.joinToString("\n"),
                                    "type" to "delete"
                                )
                            ),
                            headers = mapOf(
                                "Content-Type" to "application/json",
                                "X-DEVOPS-PROJECT-ID" to "bkci-desktop",
                                "X-DEVOPS-UID" to "autoJob"
                            )
                        )
                    }
                }.onFailure {
                    logger.warn("autoDeleteWhenSleep7Day fail ${it.message}", it)
                }
                // 云桌面通知-关机超过3天时提醒
                kotlin.runCatching {
                    workspaceService.notifyWinSleep3Day()
                }.onFailure {
                    logger.warn("notifyWinSleep3Day fail ${it.message}", it)
                }
                // 云桌面通知-未登录7天时自动降配(暂时不做)并关机
                kotlin.runCatching {
                    val readySleepWorkspace = mutableListOf<String>()
                    sleepControl.autoSleepWhenNotLogin(false, readySleepWorkspace)

                    if (readySleepWorkspace.isNotEmpty()) {
                        logger.info("read to notify system manager|$readySleepWorkspace")
                        OkhttpUtils.doPost(
                            autoDeletePipeline,
                            JsonUtil.toJson(
                                mapOf(
                                    "infos" to readySleepWorkspace.joinToString("\n"),
                                    "type" to "sleep"
                                )
                            ),
                            headers = mapOf(
                                "Content-Type" to "application/json",
                                "X-DEVOPS-PROJECT-ID" to "bkci-desktop",
                                "X-DEVOPS-UID" to "autoJob"
                            )
                        )
                    }
                }.onFailure {
                    logger.warn("autoSleepWhenNotLogin fail ${it.message}", it)
                }
                // 云桌面通知-未登录3天时提醒
                kotlin.runCatching {
                    workspaceService.notifyWinNotLogin3Day()
                }.onFailure {
                    logger.warn("notifyWinNotLogin3Day fail ${it.message}", it)
                }
                // 未达到云桌面4星活跃：自然月（排除法定节假日）内小于 5 天达到日活标准的云桌面，并邮件提醒
                kotlin.runCatching {
                    workspaceService.notifyWhenNot4StarActive()
                }.onFailure {
                    logger.warn("notifyWhenNot4StarActive fail ${it.message}", it)
                }
                // 云桌面连续14天活跃度不足10小时通知
                kotlin.runCatching {
                    workspaceService.notifyWhenNotActiveIn14Days()
                }.onFailure {
                    logger.warn("notifyWhenNotActiveIn14Days fail ${it.message}", it)
                }
            }
        } catch (e: Throwable) {
            logger.error("projectWinJob failed", e)
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
                workspaceCommon.syncStartCloudResourceList()
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
