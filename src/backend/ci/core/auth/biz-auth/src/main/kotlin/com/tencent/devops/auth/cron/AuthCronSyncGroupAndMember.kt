package com.tencent.devops.auth.cron

import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
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

    @Scheduled(cron = "0 0 8,16 * * ?")
    fun syncIamGroupMembersOfApplyRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("sync members of apply regularly | start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                permissionResourceGroupSyncService.syncIamGroupMembersOfApply()
                logger.info("sync members of apply regularly | finish")
            } else {
                logger.info("sync members of apply regularly | running")
            }
        } catch (e: Exception) {
            logger.warn("sync members of apply regularly | error", e)
        }
    }
}
