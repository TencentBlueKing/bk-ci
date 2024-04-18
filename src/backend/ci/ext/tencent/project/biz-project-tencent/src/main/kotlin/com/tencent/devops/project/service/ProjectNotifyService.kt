package com.tencent.devops.project.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.pojo.enums.ProjectRelateOBSProductStatusEnum
import com.tencent.devops.project.dao.ProjectUpdateHistoryDao
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.ProjectWithPermission
import com.tencent.devops.project.pojo.SendEmailForProjectByConditionDTO
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.ws.rs.NotFoundException

@Service
@Suppress("LongParameterList", "MaxLineLength")
class ProjectNotifyService constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val projectService: ProjectService,
    val tofService: TOFService,
    val config: CommonConfig,
    val projectUserService: ProjectUserService,
    val dslContext: DSLContext,
    val projectUpdateHistoryDao: ProjectUpdateHistoryDao,
    val redisOperation: RedisOperation
) {
    companion object {
        private val projectNotifyThreadPool = Executors.newFixedThreadPool(10)
        private val logger = LoggerFactory.getLogger(ProjectNotifyService::class.java)
        private const val NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE =
            "NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE"
        private const val NOTIFY_USER_TO_PROJECT_INFO_CHANGE =
            "NOTIFY_USER_TO_PROJECT_INFO_CHANGE"

        private const val PROJECT_INFO_CHANGE_TABLE_HEADER = """<tr><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td></tr>"""
        private const val PROJECT_INFO_CHANGE_TABLE_CONTENT_TEMPLATE = """<tr><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td><td style="border: 1px solid black; text-align: center">%s</td></tr>"""
        private const val PROJECT_ORGANIZATION_VERIFY_TEMPLATE = """<tr><td style="border: 1px solid black; text-align: center; max-widtd:300px;word-wrap: break-word; white-space: normal; ">%s</td><td style="border: 1px solid black;text-align: center; max-widtd:300px;word-wrap: break-word; white-space: normal; ">%s</td><td style="border: 1px solid black;text-align: center; max-widtd:300px;word-wrap: break-word; white-space: normal; ">%s</td><td style="border: 1px solid black;text-align: center; max-widtd:300px;word-wrap: break-word; white-space: normal; ">%s</td><td style="border: 1px solid black;text-align: center ; max-widtd:300px;word-wrap: break-word; white-space: normal;">%s</td></tr>"""
        private const val VERIFY_PROJECT_MANAGER_ORGANIZATION_BG = "verify_project_manager_organization_bg"
        private const val PROJECT_NOTIFY_USER = "project_notify_user"
        private const val IS_SEND_EMAIL_FLAG = "is_send_email_flag"
    }

    private val projectInfoShowUri = "${config.devopsHostGateway}/console/manage/%s/show"
    private val projectEditUri = "${config.devopsHostGateway}/console/manage/%s/edit"

    private val projectId2ManagerWithDeptDetail = Caffeine.newBuilder()
        .maximumSize(30000)
        .expireAfterWrite(12L, TimeUnit.HOURS)
        .build<String/*projectId*/, MutableList<UserDeptDetail>/*managerWithDeptDetail*/>()
    private val project2Manager = Caffeine.newBuilder()
        .maximumSize(30000)
        .expireAfterWrite(12L, TimeUnit.HOURS)
        .build<String/*projectId*/, List<String>/*managers*/>()

    fun sendEmailForRelatedObsByProjectIds(projectIds: List<String>): Boolean {
        logger.info("send email for related obs by projectIds:$projectIds")
        projectIds.forEach foreach@{
            val projectInfo = projectService.getByEnglishName(
                englishName = it
            ) ?: return@foreach
            if (projectInfo.productId == null && projectInfo.enabled == true) {
                sendEmailForRelatedObsByProjectId(
                    projectId = projectInfo.englishName,
                    projectName = projectInfo.projectName
                )
            }
        }
        return true
    }

    fun sendEmailForRelatedObsByCondition(
        sendEmailForProjectByConditionDTO: SendEmailForProjectByConditionDTO
    ): Boolean {
        logger.info("send email for related obs by condition:$sendEmailForProjectByConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        projectNotifyThreadPool.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            do {
                val projectInfos = projectService.listMigrateProjects(
                    migrateProjectConditionDTO = MigrateProjectConditionDTO(
                        bgId = sendEmailForProjectByConditionDTO.bgId,
                        deptId = sendEmailForProjectByConditionDTO.deptId,
                        centerId = sendEmailForProjectByConditionDTO.centerId,
                        relatedProduct = false,
                        routerTag = AuthSystemType.RBAC_AUTH_TYPE
                    ),
                    limit = limit,
                    offset = offset
                )
                logger.info("send email for related obs by condition:$offset|$limit|$projectInfos")
                if (projectInfos.isEmpty()) break
                projectInfos.forEach {
                    sendEmailForRelatedObsByProjectId(
                        projectId = it.englishName,
                        projectName = it.projectName
                    )
                }
                offset += limit
            } while (projectInfos.size == limit)
        }

        return true
    }

    private fun sendEmailForRelatedObsByProjectId(
        projectName: String,
        projectId: String
    ) {
        val managers = getProjectManager(projectId) ?: return
        val receives = managers.filterNot { projectUserService.isSeniorUser(it) }
        val bodyParams = mapOf(
            "projectName" to projectName,
            "projectInfoShowUri" to String.format(projectInfoShowUri, projectId),
            "projectEditUri" to String.format(projectEditUri, projectId)
        )
        sendEmail(
            bodyParams = bodyParams,
            receives = receives.toSet(),
            templateCode = NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE
        )
    }

    fun getProjectManager(projectId: String): List<String>? {
        return try {
            project2Manager.get(projectId) {
                client.get(ServiceProjectAuthResource::class).getProjectUsers(
                    token = tokenService.getSystemToken(),
                    projectCode = projectId,
                    group = BkAuthGroup.MANAGER
                ).data
            }
        } catch (e: Exception) {
            logger.warn("get project($projectId) managers fail ${e.message}")
            null
        }
    }

    fun getProjectsForRelatedObsByCondition(
        sendEmailForProjectByConditionDTO: SendEmailForProjectByConditionDTO
    ): Pair<Int, List<String>> {
        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE
        var count = 0
        val projectIds = mutableListOf<String>()
        do {
            val projectInfos = projectService.listMigrateProjects(
                migrateProjectConditionDTO = MigrateProjectConditionDTO(
                    bgId = sendEmailForProjectByConditionDTO.bgId,
                    deptId = sendEmailForProjectByConditionDTO.deptId,
                    centerId = sendEmailForProjectByConditionDTO.centerId,
                    relatedProduct = false,
                    routerTag = AuthSystemType.RBAC_AUTH_TYPE
                ),
                limit = limit,
                offset = offset
            )
            logger.info(
                "get project for related obs by bg condition:$sendEmailForProjectByConditionDTO|" +
                    "$offset|$limit|$projectInfos"
            )
            if (projectInfos.isEmpty()) break
            projectInfos.forEach forEach@{
                projectIds.add(it.englishName)
            }
            count += projectInfos.size
            offset += limit
        } while (projectInfos.size == limit)
        return Pair(count, projectIds)
    }

    private fun sendEmail(
        bodyParams: Map<String, String>,
        receives: Set<String>,
        templateCode: String
    ) {
        // 开关，默认打开
        val isSendEmail = redisOperation.get(IS_SEND_EMAIL_FLAG)?.toBoolean() ?: true
        if (!isSendEmail) return
        if (receives.isEmpty()) return
        // 发邮件
        val request = SendNotifyMessageTemplateRequest(
            templateCode = templateCode,
            bodyParams = bodyParams,
            titleParams = bodyParams,
            notifyType = mutableSetOf("EMAIL"),
            receivers = receives.toMutableSet()
        )
        logger.info("send email:$bodyParams|$receives")
        kotlin.runCatching {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        }.onFailure {
            logger.warn("notify email fail ${it.message}|$bodyParams|$receives")
        }
    }

    /**
     * 项目所属组织架构变更情况
    - 每天凌晨出一封邮件，汇总前一天组织架构发生变更的项目
    - 包括如下信息：项目名称、项目ID、所属组织架构-变更前、所属组织架构-变更后、操作人、操作时间
    - 项目名称 可点击，点击进入对应项目流水线列表页面
     * */
    fun sendEmailForProjectOrganizationChange(): Boolean {
        logger.info("send email for project organization change")
        val title = "项目监控-项目所属组织架构变更情况"
        var table = String.format(
            PROJECT_INFO_CHANGE_TABLE_HEADER,
            "项目名称", "项目ID", "所属组织架构-变更前", "所属组织架构-变更后", "操作人", "操作时间"
        )

        val projectInfos = projectUpdateHistoryDao.listTwentyFourHours(
            dslContext = dslContext
        ).filter { it.beforeOrganization != it.afterOrganization }
        if (projectInfos.isEmpty()) return true
        projectInfos.forEach {
            table = table.plus(
                String.format(
                    PROJECT_INFO_CHANGE_TABLE_CONTENT_TEMPLATE,
                    it.afterProjectName, it.englishName, it.beforeOrganization,
                    it.afterOrganization, it.operator, it.updatedAt
                )
            )
        }
        val bodyParams = mapOf(
            "title" to title,
            "table" to table
        )
        sendEmail(
            bodyParams = bodyParams,
            receives = getProjectNotifyManager(),
            templateCode = NOTIFY_USER_TO_PROJECT_INFO_CHANGE
        )
        return true
    }

    /**
     * 项目所属OBS运营产品变更情况
    - 每天凌晨出一封邮件，汇总前一天所属OBS运营产品发生变更的项目
    - 包括如下信息：项目名称、项目ID、所属运营产品-变更前、所属运营产品-变更后、操作人、操作时间
    - 项目名称 可点击，点击进入对应项目流水线列表页面
     * */
    fun sendEmailForProjectProductChange(): Boolean {
        logger.info("send email for project product change")
        val title = "项目监控-项目所属OBS运营产品变更情况"
        var table = String.format(
            PROJECT_INFO_CHANGE_TABLE_HEADER,
            "项目名称", "项目ID", "所属运营产品-变更前", "所属运营产品-变更后", "操作人", "操作时间"
        )

        val projectInfos = projectUpdateHistoryDao.listTwentyFourHours(dslContext = this.dslContext)
            .filter { it.beforeProductId != it.afterProductId }
        if (projectInfos.isEmpty()) return true

        projectInfos.forEach { projectInfo ->
            table = table.plus(
                String.format(
                    PROJECT_INFO_CHANGE_TABLE_CONTENT_TEMPLATE,
                    projectInfo.afterProjectName, projectInfo.englishName,
                    projectService.getOperationalProducts().firstOrNull { it.productId == projectInfo.beforeProductId }?.productName +
                        "[${projectInfo.beforeProductId}]",
                    projectService.getOperationalProducts().firstOrNull { it.productId == projectInfo.afterProductId }?.productName +
                        "[${projectInfo.afterProductId}]",
                    projectInfo.operator, projectInfo.updatedAt
                )
            )
        }
        val bodyParams = mapOf(
            "title" to title,
            "table" to table
        )
        sendEmail(
            bodyParams = bodyParams,
            receives = getProjectNotifyManager(),
            templateCode = NOTIFY_USER_TO_PROJECT_INFO_CHANGE
        )
        return true
    }

    /**
     * 监控如下场景：项目管理员所属组织架构和管理员所属组织架构是否匹配
    - 若全部管理员为 IEG 的，但项目所属组织架构不属于 IEG，发送告警邮件给系统管理员
    - 若部分管理员为 IEG 的，但项目所属组织架构不属于 IEG，需汇总到邮件中，需人工确认后，可以设置不告警
    - 邮件包括如下信息：项目名称、项目ID、项目所属组织架构、所属组织架构为IEG的管理员、所属组织架构为非IEG的管理员
    - 项目名称 可点击，点击进入对应项目流水线列表页面
     * */
    fun sendEmailForVerifyProjectOrganization(): Boolean {
        val verifyBgIds = redisOperation.get(key = VERIFY_PROJECT_MANAGER_ORGANIZATION_BG)
        if (verifyBgIds.isNullOrBlank()) {
            logger.info("verify bg ids is null or blank")
            return false
        }
        logger.info("send email for verify project organization :verifyBgIds($verifyBgIds)")
        val traceId = MDC.get(TraceTag.BIZID)
        verifyBgIds.split(",").forEach { verifyBgId ->
            projectNotifyThreadPool.submit {
                MDC.put(TraceTag.BIZID, traceId)
                var count = 0
                var offset = 0
                val limit = PageUtil.MAX_PAGE_SIZE
                // 全部管理员都归属某个BG，但项目所属组织架构不属于该BG的项目
                val wrongOrganizationalProjectList = mutableListOf<String>()
                // 项目所属组织架构不属于某个BG.但部分管理员的bg属于某个BG的项目
                val projectID2ManagerBelongVerifyBgId = mutableMapOf<String/*项目ID*/, List<String>/*管理员所属Bg为校验的verifyBgId*/>()
                val projectID2ManagerNotBelongVerifyBgId = mutableMapOf<String/*项目ID*/, List<String>/*管理员所属Bg不为校验的verifyBgId*/>()
                do {
                    val projectInfos = projectService.listMigrateProjects(
                        migrateProjectConditionDTO = MigrateProjectConditionDTO(
                            routerTag = AuthSystemType.RBAC_AUTH_TYPE
                        ),
                        limit = limit,
                        offset = offset
                    )
                    if (projectInfos.isEmpty()) break
                    processProjectInfos(
                        projectInfos = projectInfos,
                        verifyBgId = verifyBgId.toLong(),
                        wrongOrganizationalProjectList = wrongOrganizationalProjectList,
                        projectID2ManagerBelongVerifyBgId = projectID2ManagerBelongVerifyBgId,
                        projectID2ManagerNotBelongVerifyBgId = projectID2ManagerNotBelongVerifyBgId
                    )
                    count += projectInfos.size
                    offset += limit
                } while (projectInfos.size == limit)
                sendEmails(
                    verifyBgId = verifyBgId.toLong(),
                    wrongOrganizationalProjectList = wrongOrganizationalProjectList,
                    projectID2ManagerBelongVerifyBgId = projectID2ManagerBelongVerifyBgId,
                    projectID2ManagerNotBelongVerifyBgId = projectID2ManagerNotBelongVerifyBgId
                )
                logger.info("send email for verify project organization:$verifyBgId|$count")
            }
            Thread.sleep(3000L)
        }
        return true
    }

    private fun processProjectInfos(
        projectInfos: List<ProjectWithPermission>,
        verifyBgId: Long,
        wrongOrganizationalProjectList: MutableList<String>,
        projectID2ManagerBelongVerifyBgId: MutableMap<String, List<String>>,
        projectID2ManagerNotBelongVerifyBgId: MutableMap<String, List<String>>
    ) {
        projectInfos.forEach { projectInfo ->
            try {
                val managerDeptInfos = getManagerDeptInfos(projectInfo) ?: return@forEach
                val managerBgIds = managerDeptInfos.map { it.bgId }
                val isManagerBgSame = managerBgIds.distinct().size == 1
                logger.debug("process project infos:$projectInfo|$managerDeptInfos")
                if (isManagerBgSame) {
                    processManagerBgSame(
                        projectInfo = projectInfo,
                        managerBgIds = managerBgIds,
                        verifyBgId = verifyBgId,
                        wrongOrganizationalProjectList = wrongOrganizationalProjectList
                    )
                } else {
                    processManagerBgNotSame(
                        projectInfo = projectInfo,
                        managerDeptInfos = managerDeptInfos,
                        managerBgIds = managerBgIds,
                        verifyBgId = verifyBgId,
                        projectID2ManagerBelongVerifyBgId = projectID2ManagerBelongVerifyBgId,
                        projectID2ManagerNotBelongVerifyBgId = projectID2ManagerNotBelongVerifyBgId
                    )
                }
            } catch (ex: Exception) {
                logger.warn("process projectInfos fail:$ex")
            }
        }
    }

    private fun getManagerDeptInfos(projectInfo: ProjectWithPermission): MutableList<UserDeptDetail>? {
        val projectId = projectInfo.englishName
        val managerWithDeptDetail = projectId2ManagerWithDeptDetail.getIfPresent(projectId)
        if (managerWithDeptDetail != null)
            return managerWithDeptDetail

        val managerDeptInfos = mutableListOf<UserDeptDetail>()
        val managers = getProjectManager(projectId) ?: return null

        managers.forEach { manager ->
            val userDeptDetail = try {
                tofService.getUserDeptDetail(manager)
            } catch (ex: Exception) {
                logger.info("$projectId ${ex.message}")
                return@forEach
            }
            managerDeptInfos.add(userDeptDetail)
        }
        projectId2ManagerWithDeptDetail.put(projectId, managerDeptInfos)
        return managerDeptInfos
    }

    private fun processManagerBgSame(
        projectInfo: ProjectWithPermission,
        managerBgIds: List<String>,
        verifyBgId: Long,
        wrongOrganizationalProjectList: MutableList<String>
    ) {
        if (managerBgIds.first() == verifyBgId.toString()) {
            if (projectInfo.bgId != verifyBgId) {
                logger.info("process manager bg same :$managerBgIds|${projectInfo.englishName}")
                wrongOrganizationalProjectList.add(projectInfo.englishName)
            }
        }
    }

    private fun processManagerBgNotSame(
        projectInfo: ProjectWithPermission,
        managerDeptInfos: MutableList<UserDeptDetail>,
        managerBgIds: List<String>,
        verifyBgId: Long,
        projectID2ManagerBelongVerifyBgId: MutableMap<String, List<String>>,
        projectID2ManagerNotBelongVerifyBgId: MutableMap<String, List<String>>
    ) {
        if (managerBgIds.contains(verifyBgId.toString())) {
            if (verifyBgId != projectInfo.bgId) {
                projectID2ManagerBelongVerifyBgId[projectInfo.englishName] = managerDeptInfos.filter { it.bgId == verifyBgId.toString() }.map { it.userId!! }
                projectID2ManagerNotBelongVerifyBgId[projectInfo.englishName] = managerDeptInfos.filterNot { it.bgId == verifyBgId.toString() }.map { it.userId!! }
                logger.info(
                    "process manager bg not same :$managerBgIds" +
                        "|${projectID2ManagerBelongVerifyBgId[projectInfo.englishName]}" +
                        "|${projectID2ManagerNotBelongVerifyBgId[projectInfo.englishName]}"
                )
            }
        }
    }

    private fun getOrganizationStr(projectInfo: ProjectVO): String {
        return with(projectInfo) {
            listOf(
                bgName, businessLineName, deptName, centerName
            ).filter { !it.isNullOrBlank() }.joinToString("-")
        }
    }

    private fun sendEmails(
        verifyBgId: Long,
        wrongOrganizationalProjectList: MutableList<String>,
        projectID2ManagerBelongVerifyBgId: MutableMap<String, List<String>>,
        projectID2ManagerNotBelongVerifyBgId: MutableMap<String, List<String>>
    ) {
        logger.info(
            "send email for verify project manager organization:$wrongOrganizationalProjectList|" +
                "$projectID2ManagerBelongVerifyBgId|$projectID2ManagerNotBelongVerifyBgId "
        )
        val bgName = tofService.getDeptInfo(id = verifyBgId.toInt()).name

        if (wrongOrganizationalProjectList.isNotEmpty()) {
            val title1 = "项目监控-全部管理员为$bgName，但项目所属组织架构不属于${bgName}告警"
            var table1 = String.format(
                PROJECT_ORGANIZATION_VERIFY_TEMPLATE,
                "项目名称", "项目ID", "项目所属组织架构", "创建人", "管理员"
            )
            wrongOrganizationalProjectList.forEach { projectId ->
                val projectInfo = projectService.getByEnglishName(projectId) ?: return@forEach
                table1 = table1.plus(
                    String.format(
                        PROJECT_ORGANIZATION_VERIFY_TEMPLATE,
                        projectInfo.projectName, projectInfo.englishName,
                        getOrganizationStr(projectInfo), projectInfo.creator,
                        projectId2ManagerWithDeptDetail.getIfPresent(projectId)
                            ?.map { it.userId }?.joinToString(",") ?: ""
                    )
                )
            }

            val bodyParams1 = mapOf(
                "title" to title1,
                "table" to table1
            )
            sendEmail(
                bodyParams = bodyParams1,
                receives = getProjectNotifyManager(),
                templateCode = NOTIFY_USER_TO_PROJECT_INFO_CHANGE
            )
        }
        if (projectID2ManagerBelongVerifyBgId.isNotEmpty()) {
            val title2 = "项目监控-部分管理员为$bgName，但项目所属组织架构不属于${bgName}告警"
            var table2 = String.format(
                PROJECT_ORGANIZATION_VERIFY_TEMPLATE,
                "项目名称", "项目ID", "项目所属组织架构", "所属组织架构为${bgName}的管理员", "所属组织架构为非${bgName}的管理员"
            )
            projectID2ManagerBelongVerifyBgId.forEach { (projectCode, managers) ->
                val projectInfo = projectService.getByEnglishName(projectCode) ?: return@forEach
                table2 = table2.plus(
                    String.format(
                        PROJECT_ORGANIZATION_VERIFY_TEMPLATE,
                        projectInfo.projectName, projectInfo.englishName,
                        getOrganizationStr(projectInfo), projectID2ManagerBelongVerifyBgId[projectCode]!!.joinToString(","),
                        projectID2ManagerNotBelongVerifyBgId[projectCode]!!.joinToString(",")
                    )
                )
            }
            val bodyParams2 = mapOf(
                "title" to title2,
                "table" to table2
            )
            sendEmail(
                bodyParams = bodyParams2,
                receives = getProjectNotifyManager(),
                templateCode = NOTIFY_USER_TO_PROJECT_INFO_CHANGE
            )
        }
    }

    fun sendEmailsForCheckInactiveProjects(
        manager2projectList: Map<String, List<ProjectVO>>,
        project2Status: Map<String, ProjectRelateOBSProductStatusEnum>
    ) {
        if (project2Status.isEmpty())
            return
        manager2projectList.forEach foreach@{ (manager, projectList) ->
            if (projectList.isEmpty()) {
                return@foreach
            }
            var table = String.format(
                PROJECT_ORGANIZATION_VERIFY_TEMPLATE,
                "项目名称", "项目ID", "项目管理员", "项目状态", "操作"
            )
            projectList.forEach forEach@{ projectInfo ->
                with(projectInfo) {
                    val projectStatus = project2Status[englishName] ?: return@forEach
                    table = table.plus(
                        String.format(
                            PROJECT_ORGANIZATION_VERIFY_TEMPLATE,
                            projectInfo.projectName,
                            englishName,
                            getProjectManager(englishName)?.joinToString(",") ?: "",
                            projectStatus.value,
                            String.format(
                                projectStatus.action,
                                config.devopsHostGateway, englishName,
                                config.devopsHostGateway, englishName
                            )
                        )
                    )
                }
            }
            val bodyParams = mapOf(
                "count" to projectList.size.toString(),
                "table" to table
            )
            sendEmail(
                bodyParams = bodyParams,
                receives = mutableSetOf(manager),
                templateCode = NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE
            )
        }
    }

    fun getProjectNotifyManager(): Set<String> {
        val projectNotifyUser = redisOperation.get(key = PROJECT_NOTIFY_USER)
        if (projectNotifyUser.isNullOrBlank()) {
            throw NotFoundException("projectNotifyUser is null")
        }
        return projectNotifyUser.split(",").toSet()
    }
}
