package com.tencent.devops.project.service.cron

import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.service.ProjectBillsService
import com.tencent.devops.project.service.ProjectNotifyService
import com.tencent.devops.project.service.ProjectOperationalProductService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ProjectCronService constructor(
    val projectNotifyService: ProjectNotifyService,
    val projectBillsService: ProjectBillsService,
    val redisOperation: RedisOperation,
    val projectOperationalProductService: ProjectOperationalProductService
) {
    private val redisLock = RedisLock(redisOperation, PROJECT_CRON_KEY, 10)

    @Value("\${project.cron.enable:#{false}}")
    private var enable: Boolean = false

    companion object {
        private const val PROJECT_CRON_KEY = "project_cron_key"
        private val logger = LoggerFactory.getLogger(ProjectCronService::class.java)
        private const val PROCESS_INACTIVE_PROJECT_BG = "process_inactive_project_bg"
    }

    @Scheduled(cron = "0 0 6 * * ?")
    fun checkChangesOfProjectRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("check changes of project regularly |start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                projectNotifyService.sendEmailForProjectOrganizationChange()
                projectNotifyService.sendEmailForProjectProductChange()
                logger.info("check changes of project regularly |finish")
            } else {
                logger.info("check changes of project regularly |running")
            }
        } catch (e: Exception) {
            logger.warn("check changes of project regularly |error", e)
        }
    }

    /**
     * 每周一3点开启任务
     * 监控如下场景：项目管理员所属组织架构和管理员所属组织架构是否匹配
    - 若全部管理员为 IEG 的，但项目所属组织架构不属于 IEG，发送告警邮件给系统管理员
    - 若部分管理员为 IEG 的，但项目所属组织架构不属于 IEG，需汇总到邮件中，需人工确认后，可以设置不告警
    - 邮件包括如下信息：项目名称、项目ID、项目所属组织架构、所属组织架构为IEG的管理员、所属组织架构为非IEG的管理员
     * */
    @Scheduled(cron = "0 0 3 ? * MON")
    fun checkProjectOrganizationRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("check project organization regularly |start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                projectNotifyService.sendEmailForVerifyProjectOrganization()
                logger.info("check project organization regularly  |finish")
            } else {
                logger.info("check project organization regularly  |running")
            }
        } catch (e: Exception) {
            logger.warn("check project organization regularly |error", e)
        }
    }

    /**
     * 每周一8点开始检测项目活跃度
     * 该检测比较严格，会对 关联产品但不活跃/活跃但未关联产品/不关联产品且不活跃的项目进行检测，
     * 连续三周发送邮件，仍不处理，则会禁用项目
     * */
    @Scheduled(cron = "0 0 8 ? * MON")
    @Suppress("NestedBlockDepth")
    fun checkInactiveProjectRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("process inactive project regularly |start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                val bgIds = redisOperation.get(key = PROCESS_INACTIVE_PROJECT_BG)
                if (bgIds.isNullOrBlank()) {
                    logger.info("bg ids is null or blank")
                    return
                }
                projectBillsService.checkInactiveProjectRegularly(
                    projectConditionDTO = ProjectConditionDTO(
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

    /**
     * 禁用不活跃项目，不活跃定义两个月内无人访问且未执行流水线。
     * */
    @Scheduled(cron = "0 0 8 ? * SUN")
    @Suppress("NestedBlockDepth")
    fun disableInactiveProjectRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("disable inactive project regularly |start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                projectBillsService.disableInactiveProjectRegularly()
                logger.info("disable inactive project regularly |finish")
            } else {
                logger.info("disable inactive project regularly |running")
            }
        } catch (e: Exception) {
            logger.warn("disable inactive project regularly |error", e)
        }
    }

    /**
     * 每天更新OBS产品
     * */
    @Scheduled(cron = "0 0 8 ? * SUN")
    @Suppress("NestedBlockDepth")
    fun disableInactiveProjectRegularly() {
        try {
            logger.info("update obs product | start")
            projectOperationalProductService.syncOperationalProduct()
            logger.info("update obs product|finish")
        } catch (e: Exception) {
            logger.warn("update obs product | error", e)
        }
    }
}
