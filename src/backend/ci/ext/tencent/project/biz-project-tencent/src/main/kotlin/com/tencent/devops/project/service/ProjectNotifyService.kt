package com.tencent.devops.project.service

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.pojo.SendEmailForProjectByConditionDTO
import com.tencent.devops.project.service.tof.TOFService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ProjectNotifyService constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val projectService: ProjectService,
    val tofService: TOFService,
    val config: CommonConfig,
    val projectUserService: ProjectUserService
) {
    companion object {
        private val projectNotifyThreadPool = Executors.newFixedThreadPool(2)
        private val logger = LoggerFactory.getLogger(ProjectNotifyService::class.java)
        private const val NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE =
            "NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE"
    }

    private val projectInfoShowUri = "${config.devopsHostGateway}/console/manage/%s/show"
    private val projectEditUri = "${config.devopsHostGateway}/console/manage/%s/edit"

    fun sendEmailForRelatedObsByProjectIds(projectIds: List<String>): Boolean {
        logger.info("send email for related obs by projectIds:$projectIds")
        projectIds.forEach foreach@{
            val projectInfo = projectService.getByEnglishName(
                englishName = it
            ) ?: return@foreach
            if (projectInfo.productId == null && projectInfo.enabled == true) {
                sendEmail(
                    projectName = projectInfo.projectName,
                    projectId = it
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
                    sendEmail(
                        projectName = it.projectName,
                        projectId = it.englishName
                    )
                }
                offset += limit
            } while (projectInfos.size == limit)
        }

        return true
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
        projectName: String,
        projectId: String
    ) {
        val managers = client.get(ServiceProjectAuthResource::class).getProjectUsers(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            group = BkAuthGroup.MANAGER
        ).data ?: return
        val receives = managers.filterNot { projectUserService.isSeniorUser(it) }
        if (receives.isEmpty()) return

        val bodyParams = mapOf(
            "projectName" to projectName,
            "projectInfoShowUri" to String.format(projectInfoShowUri, projectId),
            "projectEditUri" to String.format(projectEditUri, projectId)
        )
        // 发邮件
        val request = SendNotifyMessageTemplateRequest(
            templateCode = NOTIFY_USER_TO_RELATED_OBS_PRODUCT_TEMPLATE_CODE,
            bodyParams = bodyParams,
            titleParams = bodyParams,
            notifyType = mutableSetOf("EMAIL"),
            receivers = receives.toMutableSet()
        )
        logger.info("send email:$projectName|$projectId|$receives")
        kotlin.runCatching {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        }.onFailure {
            logger.warn("notify email fail ${it.message}|$projectId|$receives")
        }
    }
}
