package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.service.TxMigrateService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue

class TxRbacMigrateService(
    private val client: Client,
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val resourceService: AuthResourceService
) : TxMigrateService {

    override fun migrateRemoteDevManager(projectConditionDTO: ProjectConditionDTO) {
        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE / 2
        logger.info("start to migrate remote dev manager|${projectConditionDTO}")
        do {
            val migrateProjects = client.get(ServiceProjectResource::class).listProjectsByCondition(
                projectConditionDTO = projectConditionDTO.copy(queryRemoteDevFlag = true),
                limit = limit,
                offset = offset
            ).data ?: break
            migrateProjects.forEach { projectInfo ->
                try {
                    doMigrateRemoteDevManager(projectInfo.englishName)
                } catch (ex: Exception) {
                    logger.warn("migrate remote dev manager failed |${projectInfo.englishName}| $ex")
                }
            }
            offset += limit
        } while (migrateProjects.size == limit)
    }

    override fun migrateRemoteDevManager(projectCode: String): Int {
        ThreadPoolUtil.submitAction(
            executor = executorService,
            actionTitle = "migrateRemoteDevManager-$projectCode",
            action = {
                try {
                    doMigrateRemoteDevManager(projectCode)
                } catch (ex: Exception) {
                    logger.warn("migrate remote dev manager failed |$projectCode| $ex")
                }
            }
        )
        return 0
    }

    private fun doMigrateRemoteDevManager(projectCode: String) {
        logger.info("migrate remote dev manager $projectCode")
        val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data!!
        val gradeManagerId = resourceService.get(
            projectCode = projectInfo.englishName,
            resourceType = ResourceTypeId.PROJECT,
            resourceCode = projectInfo.englishName
        ).relationId
        permissionGradeManagerService.modifyGradeManager(
            gradeManagerId = gradeManagerId,
            projectCode = projectInfo.englishName,
            projectName = projectInfo.projectName
        )
        val groupId = permissionResourceGroupService.createGroupAndPermissionsByGroupCode(
            projectId = projectInfo.englishName,
            resourceType = ResourceTypeId.PROJECT,
            resourceCode = projectInfo.englishName,
            groupCode = BkAuthGroup.CGS_MANAGER.value
        )
        projectInfo.properties?.remotedevManager?.let { managers ->
            val managerList = managers.split(";")
            managerList.forEach {
                permissionResourceMemberService.addGroupMember(
                    projectCode = projectInfo.englishName,
                    memberId = it,
                    memberType = MemberType.USER.type,
                    expiredAt = 4102444800,
                    iamGroupId = groupId
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxRbacMigrateService::class.java)
        private const val THREAD_CORE_SIZE = 5
        private const val THREAD_QUEUE_SIZE = 200
        private val executorService = ThreadPoolUtil.getThreadPoolExecutor(
            corePoolSize = THREAD_CORE_SIZE,
            maximumPoolSize = THREAD_CORE_SIZE,
            threadNamePrefix = "migrate-remote-dev-manager-%d",
            queue = LinkedBlockingQueue(THREAD_QUEUE_SIZE)
        )
    }
}
