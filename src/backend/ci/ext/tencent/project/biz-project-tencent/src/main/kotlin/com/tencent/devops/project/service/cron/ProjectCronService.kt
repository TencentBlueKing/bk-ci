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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
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
    @Scheduled(cron = "0 0 1 ? * MON")
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
     * 每周日8点开始检测项目活跃度
     * 处理逻辑：
     *（1）若项目未关联运营产品，连续两个月不活跃，则禁用；
     *（2）若项目已关联运营产品，连续四个月不活跃，则禁用；
     *（3）发邮件。
     * */
    @Scheduled(cron = "0 0 8 ? * SUN")
    @Suppress("NestedBlockDepth")
    fun checkInactiveProjectRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("process inactive project regularly |start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                projectBillsService.checkInactiveProject(projectConditionDTO = ProjectConditionDTO())
                logger.info("process inactive project regularly  |finish")
            } else {
                logger.info("process inactive project regularly  |running")
            }
        } catch (e: Exception) {
            logger.warn("process inactive project regularly  |error", e)
        }
    }

    /**
     * 每周一8点开始检测项目是否关联运营产品
     * 处理逻辑：
     * 对未关联运营产品的项目，连续三周发送邮件，若第四周还未处理的项目，则禁用。
     * */
    @Scheduled(cron = "0 0 3 ? * MON")
    fun checkProjectRelatedProductRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("check project related product regularly|start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                val bgIds = redisOperation.get(key = PROCESS_INACTIVE_PROJECT_BG)
                if (bgIds.isNullOrBlank()) {
                    logger.info("bg ids is null or blank")
                    return
                }
                projectBillsService.checkProjectRelatedProduct()
                logger.info("check project related product regularly|finish")
            } else {
                logger.info("check project related product regularly|running")
            }
        } catch (e: Exception) {
            logger.warn("check project related product regularly|error", e)
        }
    }

    /**
     * 每天更新OBS产品
     * */
    @Scheduled(cron = "0 0 3 * * ?")
    fun updateObsProduct() {
        try {
            logger.info("update obs product | start")
            projectOperationalProductService.syncOperationalProduct()
            logger.info("update obs product|finish")
        } catch (e: Exception) {
            logger.warn("update obs product | error", e)
        }
    }

    /**
     * 每个月15号凌晨0点进行上报货币化数据
     * */
    @Scheduled(cron = "0 0 0 15 * ?")
    fun reportBillDataRegularly() {
        if (!enable) {
            return
        }
        try {
            logger.info("report bill data regularly|start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                val currentYearAndMonthDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                projectBillsService.reportBillsData(
                    yearAndMonthOfReportStr = currentYearAndMonthDate
                )
                logger.info("check project related product regularly|finish")
            } else {
                logger.info("check project related product regularly|running")
            }
        } catch (e: Exception) {
            logger.warn("check project related product regularly|error", e)
        }
    }
}
