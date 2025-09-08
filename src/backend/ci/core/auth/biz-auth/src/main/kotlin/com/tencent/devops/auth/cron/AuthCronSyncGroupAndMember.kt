package com.tencent.devops.auth.cron

import com.tencent.devops.auth.pojo.enum.AuthSyncDataType
import com.tencent.devops.auth.service.UserManageService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.lock.CronSyncGroupMembersExpiredTimeLock
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
    private val permissionResourceGroupSyncService: PermissionResourceGroupSyncService,
    private val userManageService: UserManageService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService
) {
    @Value("\${sync.cron.enabled:#{false}}")
    private var enable: Boolean = false

    companion object {
        private val logger = LoggerFactory.getLogger(AuthCronSyncGroupAndMember::class.java)
    }

    @Scheduled(cron = "0 0 0 ? * SAT")
    fun syncGroupAndMemberRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("sync group and member regularly |start")
            val redisLock = RedisLock(
                redisOperation = redisOperation,
                lockKey = "sync_${AuthSyncDataType.GROUP_AND_MEMBER_SYNC_TASK_TYPE}_cron_key",
                expiredTimeInSeconds = 10
            )
            if (redisLock.tryLock()) {
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

    @Scheduled(cron = "0 0 0 ? * SAT")
    fun syncGroupPermissionsRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("sync group permissions regularly |start")
            val redisLock = RedisLock(
                redisOperation = redisOperation,
                lockKey = "sync_${AuthSyncDataType.GROUP_PERMISSIONS_SYNC_TASK_TYPE}_cron_key",
                expiredTimeInSeconds = 10
            )
            if (redisLock.tryLock()) {
                permissionResourceGroupPermissionService.syncPermissionsByCondition(
                    ProjectConditionDTO(enabled = true)
                )
                logger.info("sync group permissions regularly |finish")
            } else {
                logger.info("sync group permissions regularly |running")
            }
        } catch (e: Exception) {
            logger.warn("sync group permissions regularly |error", e)
        }
    }

    /**
     * 10秒同步一次用户申请加入组的单据，若1周未审批单据，将不再进行扫描
     * */
    @Scheduled(initialDelay = 10000, fixedRate = 10000)
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

    /**
     * 10分钟同步一次用户过期时间
     * */
    @Scheduled(initialDelay = 1000, fixedRate = 600000)
    fun syncGroupMemberExpiredTimeRegularly() {
        if (!enable) {
            return
        }
        CronSyncGroupMembersExpiredTimeLock(redisOperation).use { lock ->
            if (!lock.tryLock()) {
                logger.info("sync group member expired time regularly | running")
                return@use
            }
            try {
                logger.info("sync group member expired time regularly | start")
                permissionResourceGroupSyncService.syncGroupMemberExpiredTime(
                    ProjectConditionDTO(enabled = true)
                )
                logger.info("sync group member expired time regularly | finish")
            } catch (e: Exception) {
                logger.warn("sync group member expired time regularly| error", e)
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    fun syncUserAndDepartmentRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("sync user and department regularly |start")
            val redisLock = RedisLock(
                redisOperation = redisOperation,
                lockKey = "sync_${AuthSyncDataType.USER_SYNC_TASK_TYPE}_cron_key",
                expiredTimeInSeconds = 10
            )
            if (redisLock.tryLock()) {
                userManageService.syncAllUserInfoData()
                userManageService.syncDepartmentInfoData()
                logger.info("sync user and department regularly |finish")
            } else {
                logger.info("sync user and department regularly |running")
            }
        } catch (e: Exception) {
            logger.warn("sync user and department regularly |error", e)
        }
    }
}
