package com.tencent.devops.auth.cron

import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.lock.CronSyncGroupPermissionsLock
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AuthCronSyncGroupAndMember(
    private val redisOperation: RedisOperation,
    private val permissionResourceGroupSyncService: PermissionResourceGroupSyncService
) {
    @Value("\${sync.cron.enabled:#{false}}")
    private var enable: Boolean = false

    private val redisLock = RedisLock(redisOperation, SYNC_CRON_KEY, 10)

    companion object {
        private const val SYNC_CRON_KEY = "sync_cron_key"
        private val logger = LoggerFactory.getLogger(AuthCronSyncGroupAndMember::class.java)
    }

    @Scheduled(cron = "0 0 0 ? * SAT")
    fun syncGroupAndMemberRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("sync group and member regularly |start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                permissionResourceGroupSyncService.syncByCondition(
                    ProjectConditionDTO(enabled = true)
                )
                logger.info("sync group and member regularly |finish")
            } else {
                logger.info("sync group and member regularly |running")
            }
        } catch (e: Exception) {
            logger.warn("sync group and member regularly |error", e)
        }
    }

    /**
     * 5分钟同步一次用户申请加入组的单据，若两周未审批单据，将不再进行扫描
     * */
    @Scheduled(initialDelay = 10000, fixedRate = 300000)
    fun syncIamGroupMembersOfApplyRegularly() {
        if (!enable) {
            return
        }
        CronSyncGroupPermissionsLock(redisOperation).use { lock ->
            if (!lock.tryLock()) {
                logger.info("sync members of apply regularly | running")
                return@use
            }
            try {
                logger.info("sync members of apply regularly | start")
                permissionResourceGroupSyncService.syncIamGroupMembersOfApply()
                logger.info("sync members of apply regularly | finish")
            } catch (e: Exception) {
                logger.warn("sync members of apply regularly | error", e)
            }
        }
    }
}
