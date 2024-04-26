package com.tencent.devops.project.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.project.api.pojo.enums.ProjectRelateOBSProductStatusEnum
import com.tencent.devops.project.pojo.BkBillDTO
import com.tencent.devops.project.pojo.BkDataSourceBillsDTO
import com.tencent.devops.project.pojo.BkSummaryBillDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.BkBillKind
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
@Suppress("NestedBlockDepth", "ComplexMethod", "LongParameterList")
class ProjectBillsService constructor(
    val client: Client,
    val projectService: ProjectService,
    val redisOperation: RedisOperation,
    val projectNotifyService: ProjectNotifyService,
    val projectUserService: ProjectUserService,
    val dslContext: DSLContext
) {
    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val projectCostAllocationThreadPool = Executors.newFixedThreadPool(2)
        private val logger = LoggerFactory.getLogger(ProjectBillsService::class.java)
        private const val NUMBER_OF_PROJECT_ACTIVITY_CHECKS_KEY = "number_of_project_activity_checks_%s"
        private const val PROCESS_INACTIVE_PROJECT_BG = "process_inactive_project_bg"
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

    @Value("\${bill.url:#{null}}")
    private var billUrl: String = ""

    @Value("\${bill.key:#{null}}")
    private var billKey: String = ""

    fun checkInactiveProjectRegularly(
        projectConditionDTO: ProjectConditionDTO
    ): Boolean {
        logger.info("Checking inactive projects start |$projectConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        projectCostAllocationThreadPool.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            var totalCount = 0
            do {
                val projectInfos = projectService.listProjectsByCondition(
                    projectConditionDTO = projectConditionDTO.copy(
                        excludedCreateTime = LocalDate.now().minusMonths(2).format(DATE_FORMATTER),
                        routerTag = AuthSystemType.RBAC_AUTH_TYPE,
                        enabled = true
                    ),
                    limit = limit,
                    offset = offset
                )
                if (projectInfos.isEmpty()) break
                logger.debug("check inactive projects:${projectInfos.map { it.englishName }}")
                projectInfos.forEach {
                    checkInactiveProjectRegularly(it.englishName)
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

    fun checkInactiveProjectRegularly(projectList: List<String>): Boolean {
        projectCostAllocationThreadPool.submit {
            projectList.forEach {
                checkInactiveProjectRegularly(it)
            }
            // Send email
            projectNotifyService.sendEmailsForCheckInactiveProjects(
                project2Status = project2Status.asMap(),
                manager2projectList = manager2projectList.asMap()
            )
        }
        return true
    }

    fun checkInactiveProjectRegularly(englishName: String) {
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

    // 若项目两个月内无人访问并且未执行过流水线，则判定为不活跃项目
    private fun isProjectActive(projectId: String): Boolean {
        val startTime = LocalDate.now().minusMonths(2).format(DATE_FORMATTER)
        val endTime = LocalDate.now().format(DATE_FORMATTER)
        val projectActiveUserCount = client.get(ServiceMetricsResource::class).getProjectActiveUserCount(
            BaseQueryReqVO(
                projectId = projectId,
                startTime = startTime,
                endTime = endTime
            )
        ).data?.userCount ?: 0

        val totalExecuteCount = client.get(ServiceMetricsResource::class).queryPipelineSumInfo(
            projectId = projectId,
            userId = "devops-admin",
            baseQueryReq = BaseQueryReqVO(
                startTime = LocalDate.now().minusMonths(2).format(DATE_FORMATTER),
                endTime = LocalDate.now().format(DATE_FORMATTER)
            )
        ).data?.pipelineSumInfoDO?.totalExecuteCount ?: 0
        return projectActiveUserCount != 0 && totalExecuteCount != 0L
    }

    fun reportBillsData(): Boolean {
        val traceId = MDC.get(TraceTag.BIZID)
        projectCostAllocationThreadPool.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = 10
            var count = 0
            val currentDate = LocalDate.now()
            val targetDate = LocalDate.of(currentDate.year, currentDate.monthValue, 14)
            val previousDate = if (currentDate.monthValue == 1) {
                LocalDate.of(currentDate.year - 1, 12, 15)
            } else {
                LocalDate.of(currentDate.year, currentDate.monthValue - 1, 15)
            }

            do {
                val projects = projectService.listProjectsByCondition(
                    projectConditionDTO = ProjectConditionDTO(
                        routerTag = AuthSystemType.RBAC_AUTH_TYPE,
                        enabled = true
                    ),
                    limit = limit,
                    offset = offset
                )
                logger.info("report bills data:$offset|$limit|$projects")
                if (projects.isEmpty()) break
                val bills = mutableListOf<BkBillDTO>()
                projects.forEach forEach@{
                    try {
                        val projectInfo = projectService.getByEnglishName(it.englishName) ?: return@forEach
                        // 若项目不活跃（无人访问），不上报
                        val projectActiveUserCount = client.get(ServiceMetricsResource::class).getProjectActiveUserCount(
                            BaseQueryReqVO(
                                projectId = it.englishName,
                                startTime = previousDate.format(DATE_FORMATTER),
                                endTime = targetDate.format(DATE_FORMATTER)
                            )
                        ).data?.userCount ?: return@forEach
                        val maxJobConcurrency = client.get(ServiceMetricsResource::class).getMaxJobConcurrency(
                            BaseQueryReqVO(
                                projectId = it.englishName,
                                startTime = previousDate.format(DATE_FORMATTER),
                                endTime = targetDate.format(DATE_FORMATTER)
                            )
                        ).data
                        val billKind2Usage = mapOf(
                            BkBillKind.DOCKER_VM to (maxJobConcurrency?.dockerVm ?: 0),
                            BkBillKind.DOCKER_DEVCLOUD to (maxJobConcurrency?.dockerDevcloud ?: 0),
                            BkBillKind.MACOS_DEVCLOUD to (maxJobConcurrency?.macosDevcloud ?: 0),
                            BkBillKind.WINDOWS_DEVCLOUD to (maxJobConcurrency?.windowsDevcloud ?: 0),
                            BkBillKind.BUILD_LESS to (maxJobConcurrency?.buildLess ?: 0),
                            BkBillKind.PRIVATE to (maxJobConcurrency?.other ?: 0),
                            BkBillKind.PIPELINE_USER_COUNT to projectActiveUserCount
                        )
                        billKind2Usage.forEach { (billKind, usage) ->
                            val bkBillDTO = BkBillDTO(
                                costDate = currentDate.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM")),
                                projectId = it.englishName,
                                kind = billKind.name,
                                usage = usage,
                                bgName = projectInfo.bgName ?: "",
                                flag = projectInfo.productId != null
                            )
                            bills.add(bkBillDTO)
                        }
                        val dataSourceBillsDTO = BkDataSourceBillsDTO(
                            dataSourceName = "蓝盾流水线",
                            bills = bills
                        )
                        val summaryBillDTO = BkSummaryBillDTO(
                            dataSourceBills = dataSourceBillsDTO
                        )
                        // 上报数据至saas
                        reportBillsDataToSaas(summaryBillDTO = summaryBillDTO)
                        logger.info("report bills data:$summaryBillDTO")
                        count += 1
                    } catch (ignore: Exception) {
                        logger.warn("report bills data failed!${ignore.message}|${it.englishName}")
                    }
                }
                offset += limit
            } while (projects.size == limit)
            logger.info("report bills data total :$count")
        }
        return true
    }

    private fun reportBillsDataToSaas(summaryBillDTO: BkSummaryBillDTO) {
        try {
            val requestBody = JsonUtils.objectMapper.writeValueAsString(summaryBillDTO)
            OkhttpUtils.doPost(
                url = billUrl,
                jsonParam = requestBody,
                headers = mapOf("Platform-Key" to billKey)
            ).use {
                if (!it.isSuccessful) {
                    logger.warn("request bill data failed,response:($it)")
                    throw RemoteServiceException("request failed, response:($it)")
                }
            }
        } catch (ignore: Exception) {
            logger.warn("request bill data failed!${ignore.message}")
        }
    }

    fun disableInactiveProjectRegularly(): Boolean {
        logger.info("Checking inactive projects start")
        val traceId = MDC.get(TraceTag.BIZID)
        projectCostAllocationThreadPool.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            var totalCount = 0
            val excludeBgIds = redisOperation.get(key = PROCESS_INACTIVE_PROJECT_BG)?.split(",")?.map { it.toLong() }
                ?: emptyList()
            do {
                val projects = projectService.listProjectsByCondition(
                    projectConditionDTO = ProjectConditionDTO(
                        excludedCreateTime = LocalDate.now().minusMonths(2).format(DATE_FORMATTER),
                        routerTag = AuthSystemType.RBAC_AUTH_TYPE,
                        enabled = true,
                        relatedProduct = false
                    ),
                    limit = limit,
                    offset = offset
                )
                if (projects.isEmpty()) break

                projects.forEach {
                    val projectInfo = projectService.getByEnglishName(it.englishName) ?: return@forEach
                    // 对于已经参与项目活跃度检查的BG，不参与不活跃项目停用监控
                    if (excludeBgIds.contains(it.bgId)) return@forEach
                    // 已关联产品的项目，不做禁用
                    if (projectInfo.productId != null) return@forEach
                    // 若备案为不被禁用项目，不做禁用
                    val isDisableWhenInactive = projectInfo.properties?.disableWhenInactive
                    if (isDisableWhenInactive == false) {
                        logger.info(
                            "the project(${it.englishName}) not allowed to disable" +
                                " when the project is inactive"
                        )
                        return@forEach
                    }
                    if (!isProjectActive(it.englishName)) {
                        // todo 先上线确认代码有没有问题
                        /*projectService.updateUsableStatus(
                            englishName = it.englishName,
                            enabled = false,
                            checkPermission = false
                        )*/
                        disabledProjectList.add(it.englishName)
                        totalCount += 1
                    }
                }
                offset += limit
            } while (projects.size == limit)
            logger.info("Disable inactive projects finished, total: $totalCount|$disabledProjectList")
            disabledProjectList.clear()
        }
        return true
    }
}
