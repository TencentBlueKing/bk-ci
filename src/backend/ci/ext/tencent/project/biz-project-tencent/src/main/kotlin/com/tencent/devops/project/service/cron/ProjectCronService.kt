package com.tencent.devops.project.service.cron

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.service.ProjectNotifyService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ProjectCronService constructor(
    val projectNotifyService: ProjectNotifyService,
    val redisOperation: RedisOperation
) {
    companion object {
        private const val CHECK_CHANGES_OF_PROJECT_REGULARLY = "check_changes_of_project_regularly"
        private val logger = LoggerFactory.getLogger(ProjectCronService::class.java)
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
}
