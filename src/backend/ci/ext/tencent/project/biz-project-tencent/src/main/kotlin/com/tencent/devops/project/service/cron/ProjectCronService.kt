package com.tencent.devops.project.service.cron

import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.service.ProjectCostAllocationService
import com.tencent.devops.project.service.ProjectNotifyService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ProjectCronService constructor(
    val projectNotifyService: ProjectNotifyService,
    val projectCostAllocationService: ProjectCostAllocationService,
    val redisOperation: RedisOperation
) {
    companion object {
        private const val CHECK_CHANGES_OF_PROJECT_REGULARLY = "check_changes_of_project_regularly"
        private const val PROCESS_INACTIVE_PROJECT_REGULARLY = "process_inactive_project_regularly"
        private val logger = LoggerFactory.getLogger(ProjectCronService::class.java)
        private const val VERIFY_PROJECT_MANAGER_ORGANIZATION_BG = "verify_project_manager_organization_bg"
    }

    @Scheduled(cron = "0 0 3 * * ?")
    fun checkChangesOfProjectRegularly() {
        RedisLock(redisOperation, CHECK_CHANGES_OF_PROJECT_REGULARLY, 10).use { redisLock ->
            try {
                logger.info("check changes of project regularly |start")
                val lockSuccess = redisLock.tryLock()
                if (lockSuccess) {
                    projectNotifyService.sendEmailForProjectOrganizationChange()
                    projectNotifyService.sendEmailForProjectProductChange()
                    projectNotifyService.sendEmailForVerifyProjectOrganization()
                    logger.info("check changes of project regularly |finish")
                } else {
                    logger.info("check changes of project regularly |running")
                }
            } catch (e: Exception) {
                logger.warn("check changes of project regularly |error", e)
            }
        }
    }

    // 每周一8点开始检测项目活跃度
    @Scheduled(cron = "0 0 8 ? * MON")
    @Suppress("NestedBlockDepth")
    fun processInactiveProjectRegularly() {
        RedisLock(redisOperation, PROCESS_INACTIVE_PROJECT_REGULARLY, 10).use { redisLock ->
            try {
                logger.info("process inactive project regularly |start")
                val lockSuccess = redisLock.tryLock()
                if (lockSuccess) {
                    val bgIds = redisOperation.get(key = VERIFY_PROJECT_MANAGER_ORGANIZATION_BG)
                    if (bgIds.isNullOrBlank()) {
                        logger.info("bg ids is null or blank")
                        return
                    }
                    projectCostAllocationService.processInactiveProjectByCondition(
                        migrateProjectConditionDTO = MigrateProjectConditionDTO(
                            bgIdList = bgIds.split(",").map { it.toLong() }
                        )
                    )
                    logger.info("process inactive project regularly  |finish")
                } else {
                    logger.info("process inactive project regularly  |running")
                }
            } catch (e: Exception) {
                logger.warn("process inactive project regularly  |error", e)
            }
        }
    }
}
