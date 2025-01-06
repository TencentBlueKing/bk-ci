package com.tencent.devops.project.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.RetryUtils
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
class ProjectBillsService(
    val client: Client,
    val projectService: ProjectService,
    val redisOperation: RedisOperation,
    val projectNotifyService: ProjectNotifyService,
    val projectUserService: ProjectUserService,
    val dslContext: DSLContext,
    val objectMapper: ObjectMapper,
    val projectPaasCCService: ProjectPaasCCService
) {
    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val projectBillThreadPool = Executors.newSingleThreadExecutor()
        private val logger = LoggerFactory.getLogger(ProjectBillsService::class.java)
        private const val NUMBER_OF_PROJECT_NOT_RELATED_PRODUCT_KEY = "number_of_project_not_related_product_%s"
        private const val NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE =
            "NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE"
        private const val PROJECT_ACTIVITY_CHECK_TEMPLATE_CODE = "PROJECT_ACTIVITY_CHECK_TEMPLATE_CODE"
        private const val IS_DISABLE_FLAG = "is_disable_flag"
        private const val IEG_BG_ID = 956L
        private const val BILL_DATA_SOURCE_NAME = "蓝盾服务货币化"
        private const val BILL_DATA_SERVICE_TYPE = "流水线服务"
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

    @Value("\${bill.limit:#{null}}")
    private var billLimit: Int = 30
    fun checkInactiveProject(projectConditionDTO: ProjectConditionDTO): Boolean {
        logger.info("Checking inactive projects start |$projectConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        projectBillThreadPool.submit {
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
                    checkInactiveProject(it.englishName)
                }
                offset += limit
                totalCount += projectInfos.size
            } while (projectInfos.size == limit)
            // Send email
            projectNotifyService.sendEmailsForCheckProjects(
                project2Status = project2Status.asMap(),
                manager2projectList = manager2projectList.asMap(),
                templateCode = PROJECT_ACTIVITY_CHECK_TEMPLATE_CODE
            )
            logger.info("Disable inactive projects finished, total: $totalCount|$disabledProjectList|$project2Status")
            clearCacheAfterCheck()
        }
        return true
    }

    fun checkInactiveProject(projectList: List<String>): Boolean {
        projectBillThreadPool.submit {
            projectList.forEach {
                checkInactiveProject(it)
            }
            // Send email
            projectNotifyService.sendEmailsForCheckProjects(
                project2Status = project2Status.asMap(),
                manager2projectList = manager2projectList.asMap(),
                templateCode = PROJECT_ACTIVITY_CHECK_TEMPLATE_CODE
            )
            logger.info("check inactive project finished|$disabledProjectList")
            clearCacheAfterCheck()
        }
        return true
    }

    private fun checkInactiveProject(englishName: String) {
        try {
            val projectInfo = projectService.getByEnglishName(englishName) ?: return
            // 若备案为不被禁用项目，则不做禁用
            val isDisableWhenInactive = projectInfo.properties?.disableWhenInactive
            if (isDisableWhenInactive == false) {
                logger.info(
                    "the project($englishName) not allowed to disable" +
                        " when the project is inactive"
                )
                return
            }

            val isActive = isProjectActive(projectInfo)
            if (isActive) {
                logger.info("The project{$englishName} is active and does not need to be processed")
                return
            }

            val status = calculateProjectStatus(projectInfo.productId)
            logger.info("project($englishName) status is ${status.value}")
            val isDisableFlag = redisOperation.get(IS_DISABLE_FLAG)?.toBoolean() ?: false
            if (isDisableFlag) {
                projectService.updateUsableStatus(
                    englishName = englishName,
                    enabled = false,
                    checkPermission = false
                )
            }
            project2Status.put(englishName, status)
            addProjectToManagerList(projectInfo)
            disabledProjectList.add(englishName)
        } catch (ex: Exception) {
            logger.warn("process inactive project failed: $englishName | ${ex.message}")
        }
    }

    fun checkProjectRelatedProduct(): Boolean {
        logger.info("check project related product start")
        val traceId = MDC.get(TraceTag.BIZID)
        projectBillThreadPool.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            var totalCount = 0
            do {
                val projectInfos = projectService.listProjectsByCondition(
                    projectConditionDTO = ProjectConditionDTO(
                        routerTag = AuthSystemType.RBAC_AUTH_TYPE,
                        enabled = true,
                        relatedProduct = false,
                        bgIdList = listOf(IEG_BG_ID)
                    ),
                    limit = limit,
                    offset = offset
                )
                if (projectInfos.isEmpty()) break
                logger.debug("check inactive projects:${projectInfos.map { it.englishName }}")
                projectInfos.forEach {
                    checkProjectRelatedProduct(it.englishName)
                }
                offset += limit
                totalCount += projectInfos.size
            } while (projectInfos.size == limit)
            // Send email
            projectNotifyService.sendEmailsForCheckProjects(
                project2Status = project2Status.asMap(),
                manager2projectList = manager2projectList.asMap(),
                templateCode = NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE
            )
            logger.info("check project related product finished, total: $totalCount|$disabledProjectList")
            clearCacheAfterCheck()
        }
        return true
    }

    fun checkProjectRelatedProduct(projectList: List<String>): Boolean {
        projectBillThreadPool.submit {
            projectList.forEach {
                checkProjectRelatedProduct(it)
            }
            // Send email
            projectNotifyService.sendEmailsForCheckProjects(
                project2Status = project2Status.asMap(),
                manager2projectList = manager2projectList.asMap(),
                templateCode = NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE
            )
            logger.info("check project related product finished|$disabledProjectList")
            clearCacheAfterCheck()
        }
        return true
    }

    private fun checkProjectRelatedProduct(englishName: String) {
        try {
            val projectInfo = projectService.getByEnglishName(englishName) ?: return
            // 若备案为不被禁用项目，则不做禁用
            val isDisableWhenInactive = projectInfo.properties?.disableWhenInactive
            if (isDisableWhenInactive == false) {
                logger.info(
                    "the project($englishName) not allowed to disable" +
                        " when the project is inactive"
                )
                return
            }
            val isDisableFlag = redisOperation.get(IS_DISABLE_FLAG)?.toBoolean() ?: false
            if (isDisableFlag) {
                if (shouldDisableProject(englishName)) {
                    disableProject(englishName)
                    return
                }
                project2Status.put(englishName, ProjectRelateOBSProductStatusEnum.NOT_RELATE_PRODUCT)
                addProjectToManagerList(projectInfo)
                updateProjectNotRelatedProductCheck(englishName)
            } else {
                disabledProjectList.add(englishName)
            }
        } catch (ex: Exception) {
            logger.warn("check project related product failed: $englishName | ${ex.message}")
        }
    }

    private fun calculateProjectStatus(productId: Int?): ProjectRelateOBSProductStatusEnum {
        return when {
            // 已关联OBS运营产品，但已有4个月不活跃
            productId != null -> ProjectRelateOBSProductStatusEnum.RELATE_PRODUCT_BUT_INACTIVE
            // 未关联OBS运营产品，且已有2个月不活跃
            else -> ProjectRelateOBSProductStatusEnum.INACTIVE_AND_NOT_RELATE_PRODUCT
        }
    }

    private fun shouldDisableProject(englishName: String): Boolean {
        val key = String.format(NUMBER_OF_PROJECT_NOT_RELATED_PRODUCT_KEY, englishName)
        val numberOfProjectActivityCheck = redisOperation.get(key)?.toInt() ?: 0
        logger.info("number of project not related check: $key | $numberOfProjectActivityCheck")
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
        val key = String.format(NUMBER_OF_PROJECT_NOT_RELATED_PRODUCT_KEY, englishName)
        redisOperation.delete(key)
        disabledProjectList.add(englishName)
    }

    private fun updateProjectNotRelatedProductCheck(englishName: String) {
        val key = String.format(NUMBER_OF_PROJECT_NOT_RELATED_PRODUCT_KEY, englishName)
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

    /**
     *  项目不活跃的判定：BCS项目判定为不活跃，并且
     *  项目虽已关联运营产品，但4个月内无人访问并且没有执行过流水线则为不活跃；
     *  或者项目未关联运营产品，且2个月内无人访问并且没有执行过流水线则为不活跃
     * */
    private fun isProjectActive(projectInfo: ProjectVO): Boolean {
        // 校验BCS项目是否活跃
        val isPassCCProjectActive = projectPaasCCService.checkPassCCProjectActivity(
            projectCode = projectInfo.englishName
        )
        if (isPassCCProjectActive)
            return true

        // 校验项目是否有访问过/执行过流水线
        val monthsToSubtract = if (projectInfo.productId == null) 2L else 4L
        val startTime = LocalDate.now().minusMonths(monthsToSubtract).format(DATE_FORMATTER)
        val endTime = LocalDate.now().format(DATE_FORMATTER)
        val projectId = projectInfo.englishName
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
                startTime = startTime,
                endTime = endTime
            )
        ).data?.pipelineSumInfoDO?.totalExecuteCount ?: 0
        return projectActiveUserCount != 0 || totalExecuteCount != 0L
    }

    private fun clearCacheAfterCheck() {
        disabledProjectList.clear()
        project2Status.invalidateAll()
        manager2projectList.invalidateAll()
    }

    fun reportBillsData(yearAndMonthOfReportStr: String): Boolean {
        val traceId = MDC.get(TraceTag.BIZID)
        projectBillThreadPool.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = billLimit
            var count = 0
            val yearAndMonthOfReportDate = LocalDate.parse(
                yearAndMonthOfReportStr + "01", DateTimeFormatter.ofPattern("yyyyMMdd")
            )
            val startTime = if (yearAndMonthOfReportDate.monthValue == 1) {
                LocalDate.of(yearAndMonthOfReportDate.year - 1, 12, 15)
            } else {
                LocalDate.of(yearAndMonthOfReportDate.year, yearAndMonthOfReportDate.monthValue - 1, 15)
            }
            val endTime = LocalDate.of(yearAndMonthOfReportDate.year, yearAndMonthOfReportDate.monthValue, 15)
            do {
                val projects = projectService.listProjectsByCondition(
                    projectConditionDTO = ProjectConditionDTO(
                        routerTag = AuthSystemType.RBAC_AUTH_TYPE
                    ),
                    limit = limit,
                    offset = offset
                )
                logger.info("report bills data:$offset|$limit|$projects")
                if (projects.isEmpty()) break
                val bills = mutableListOf<BkBillDTO>()
                projects.forEach forEach@{
                    val projectInfo = projectService.getByEnglishName(it.englishName) ?: return@forEach
                    // 若项目不活跃（无人访问），不上报
                    val projectActiveUserResponse = client.get(ServiceMetricsResource::class)
                        .getProjectActiveUserCount(
                            BaseQueryReqVO(
                                projectId = it.englishName,
                                startTime = startTime.format(DATE_FORMATTER),
                                endTime = endTime.format(DATE_FORMATTER)
                            )
                        ).data ?: return@forEach
                    // 不活跃项目不上报
                    if (projectActiveUserResponse.userCount == 0)
                        return@forEach

                    val maxJobConcurrency = client.get(ServiceMetricsResource::class).getMaxJobConcurrency(
                        BaseQueryReqVO(
                            projectId = it.englishName,
                            startTime = startTime.format(DATE_FORMATTER),
                            endTime = endTime.format(DATE_FORMATTER)
                        )
                    ).data
                    val billKind2Usage = mapOf(
                        BkBillKind.DOCKER_VM to (maxJobConcurrency?.dockerVm ?: 0),
                        BkBillKind.DOCKER_DEVCLOUD to (maxJobConcurrency?.dockerDevcloud ?: 0),
                        BkBillKind.MACOS_DEVCLOUD to (maxJobConcurrency?.macosDevcloud ?: 0),
                        BkBillKind.WINDOWS_DEVCLOUD to (maxJobConcurrency?.windowsDevcloud ?: 0),
                        BkBillKind.BUILD_LESS to (maxJobConcurrency?.buildLess ?: 0),
                        BkBillKind.PRIVATE to (maxJobConcurrency?.other ?: 0),
                        BkBillKind.PIPELINE_USER_COUNT to projectActiveUserResponse.userCount
                    )
                    billKind2Usage.forEach { (billKind, usage) ->
                        if (usage != 0) {
                            val bkBillDTO = BkBillDTO(
                                costDate = yearAndMonthOfReportStr,
                                projectId = it.englishName,
                                projectName = it.projectName,
                                serviceType = BILL_DATA_SERVICE_TYPE,
                                kind = billKind.name,
                                usage = usage,
                                bgName = projectInfo.bgName ?: "",
                                flag = projectInfo.productId != null && projectInfo.bgId == IEG_BG_ID.toString()
                            )
                            // 若是流水线用户类型，还额外需要上报用户名单
                            if (billKind == BkBillKind.PIPELINE_USER_COUNT) {
                                bkBillDTO.users = projectActiveUserResponse.users
                            }
                            bills.add(bkBillDTO)
                        }
                    }
                    count += 1
                }
                val dataSourceBillsDTO = BkDataSourceBillsDTO(
                    dataSourceName = BILL_DATA_SOURCE_NAME,
                    bills = bills,
                    month = yearAndMonthOfReportStr
                )
                val summaryBillDTO = BkSummaryBillDTO(
                    dataSourceBills = dataSourceBillsDTO,
                    overwrite = true
                )
                // 上报数据至saas
                reportBillsDataToSaas(summaryBillDTO = summaryBillDTO)
                logger.info("report bills summary data :$summaryBillDTO")
                offset += limit
            } while (projects.size == limit)
            logger.info("report bills data total :$count")
        }
        return true
    }

    private fun reportBillsDataToSaas(summaryBillDTO: BkSummaryBillDTO) {
        val reportProjects = summaryBillDTO.dataSourceBills.bills.map { it.projectId }
        if (reportProjects.isEmpty()) return
        try {
            RetryUtils.retry(3) {
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
                    val responseStr = it.body!!.string()
                    val responseDTO = objectMapper.readValue(
                        responseStr,
                        object : TypeReference<ResponseDTO<Map<Any, Any>>>() {})
                    if (responseDTO.code != 200L || !responseDTO.result) {
                        // 请求错误
                        logger.warn("request failed, message:(${responseDTO.message})")
                        throw RemoteServiceException("request failed, response:(${responseDTO.message})")
                    }
                    logger.info("report bills data to saas success!|$reportProjects|$responseStr")
                }
            }
        } catch (ignore: Exception) {
            logger.warn("request bill data failed!${ignore.message}|$reportProjects")
        }
    }
}
