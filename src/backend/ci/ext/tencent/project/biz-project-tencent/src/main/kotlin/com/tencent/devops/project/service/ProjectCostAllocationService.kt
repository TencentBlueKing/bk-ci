package com.tencent.devops.project.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.project.api.pojo.enums.ProjectRelateOBSProductStatusEnum
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
@Suppress("NestedBlockDepth", "ComplexMethod")
class ProjectCostAllocationService constructor(
    val client: Client,
    val projectService: ProjectService,
    val redisOperation: RedisOperation,
    val tokenService: ClientTokenService,
    val projectNotifyService: ProjectNotifyService,
    val projectUserService: ProjectUserService
) {
    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val projectCostAllocationThreadPool = Executors.newFixedThreadPool(2)
        private val logger = LoggerFactory.getLogger(ProjectCostAllocationService::class.java)
        private const val NUMBER_OF_PROJECT_ACTIVITY_CHECKS_KEY = "number_of_project_activity_checks_%s"
    }

    private val project2Status = Caffeine.newBuilder()
        .maximumSize(30000)
        .expireAfterWrite(12L, TimeUnit.HOURS)
        .build<String/*project*/, ProjectRelateOBSProductStatusEnum/*status*/>()
    private val manager2projectList = Caffeine.newBuilder()
        .maximumSize(30000)
        .expireAfterWrite(12L, TimeUnit.HOURS)
        .build<String/*manager*/, MutableList<ProjectVO>/*projectList*/>()
    private val disabledProjectList = mutableListOf<String>()

    fun processInactiveProjectByCondition(
        migrateProjectConditionDTO: MigrateProjectConditionDTO
    ): Boolean {
        logger.info("Checking inactive projects start |$migrateProjectConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        projectCostAllocationThreadPool.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            var totalCount = 0
            do {
                val projectInfos = projectService.listMigrateProjects(
                    migrateProjectConditionDTO = migrateProjectConditionDTO.copy(
                        excludedCreateTime = LocalDate.now().minusMonths(2).format(DATE_FORMATTER),
                        routerTag = AuthSystemType.RBAC_AUTH_TYPE
                    ),
                    limit = limit,
                    offset = offset
                )
                if (projectInfos.isEmpty()) break
                logger.debug("check inactive projects:${projectInfos.map { it.englishName }}")
                projectInfos.forEach {
                    processInactiveProject(it.englishName)
                }
                offset += limit
                totalCount += projectInfos.size
            } while (projectInfos.size == limit)
            // Send email
            projectNotifyService.sendEmailsForCheckInactiveProjects(
                project2Status = project2Status.asMap(),
                manager2projectList = manager2projectList.asMap()
            )
            logger.info("Disable inactive projects finished, total: $totalCount|$disabledProjectList")
            disabledProjectList.clear()
        }
        return true
    }

    fun processInactiveProject(projectList: List<String>): Boolean {
        projectCostAllocationThreadPool.submit {
            projectList.forEach {
                processInactiveProject(it)
            }
            // Send email
            projectNotifyService.sendEmailsForCheckInactiveProjects(
                project2Status = project2Status.asMap(),
                manager2projectList = manager2projectList.asMap()
            )
        }
        return true
    }

    fun processInactiveProject(englishName: String) {
        try {
            val projectInfo = projectService.getByEnglishName(englishName) ?: return

            val isActive = isProjectActive(englishName)
            if (isActive && projectInfo.productId != null) {
                logger.info("project($englishName) status is active and related product")
                return
            }

            val status = calculateProjectStatus(projectInfo.productId, isActive)
            logger.info("project($englishName) status is ${status.value}")

            project2Status.put(englishName, status)

            // 连续四周检测项目不合格，需要禁用项目
            if (shouldDisableProject(englishName)) {
                disableProject(englishName)
                return
            }

            updateProjectActivityCheck(englishName)

            addProjectToManagerList(projectInfo)
        } catch (ex: Exception) {
            logger.warn("process inactive project failed: $englishName | ${ex.message}")
        }
    }

    private fun calculateProjectStatus(productId: Int?, isActive: Boolean): ProjectRelateOBSProductStatusEnum {
        return when {
            // 活跃但未关联产品
            isActive -> ProjectRelateOBSProductStatusEnum.ACTIVE_BUT_NOT_RELATE_PRODUCT
            // 关联产品但不活跃
            productId != null -> ProjectRelateOBSProductStatusEnum.RELATE_PRODUCT_BUT_INACTIVE
            // 未关联产品且不活跃
            else -> ProjectRelateOBSProductStatusEnum.INACTIVE_AND_NOT_RELATE_PRODUCT
        }
    }

    private fun shouldDisableProject(englishName: String): Boolean {
        val key = String.format(NUMBER_OF_PROJECT_ACTIVITY_CHECKS_KEY, englishName)
        val numberOfProjectActivityCheck = redisOperation.get(key)?.toInt() ?: 0
        logger.info("number of project activity check: $key | $numberOfProjectActivityCheck")
        // 该项目已被检测过三次，此次为第四次检测，应该被禁用
        return numberOfProjectActivityCheck == 3
    }

    private fun disableProject(englishName: String) {
        logger.info("disable inactive project: $englishName")
        projectService.updateUsableStatus(
            englishName = englishName,
            enabled = false,
            checkPermission = false
        )
        val key = String.format(NUMBER_OF_PROJECT_ACTIVITY_CHECKS_KEY, englishName)
        redisOperation.delete(key)
        project2Status.invalidate(englishName)
        disabledProjectList.add(englishName)
    }

    private fun updateProjectActivityCheck(englishName: String) {
        val key = String.format(NUMBER_OF_PROJECT_ACTIVITY_CHECKS_KEY, englishName)
        val numberOfProjectActivityCheck = redisOperation.get(key)?.toInt() ?: 0
        redisOperation.set(
            key, (numberOfProjectActivityCheck + 1).toString(),
            TimeUnit.DAYS.toSeconds(30)
        )
    }

    private fun addProjectToManagerList(projectInfo: ProjectVO) {
        val managers = projectNotifyService.getProjectManager(projectInfo.englishName)
            ?.filterNot { projectUserService.isSeniorUser(it) }
            ?: return

        managers.forEach { manager ->
            val projectList = manager2projectList.getIfPresent(manager) ?: mutableListOf()
            if (projectList.find { it.englishName == projectInfo.englishName } == null) {
                projectList.add(projectInfo)
                manager2projectList.put(manager, projectList)
            }
        }
    }

    private fun isProjectActive(projectId: String): Boolean {
        val totalExecuteCount = client.get(ServiceMetricsResource::class).queryPipelineSumInfo(
            projectId = projectId,
            userId = "devops-admin",
            baseQueryReq = BaseQueryReqVO(
                startTime = LocalDate.now().minusMonths(2).format(DATE_FORMATTER),
                endTime = LocalDate.now().format(DATE_FORMATTER)
            )
        ).data?.pipelineSumInfoDO?.totalExecuteCount ?: 0
        return totalExecuteCount != 0L
    }
}
