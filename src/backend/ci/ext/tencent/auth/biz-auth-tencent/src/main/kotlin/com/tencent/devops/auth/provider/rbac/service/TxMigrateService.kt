package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectByConditionDTO
import org.slf4j.LoggerFactory

class TxMigrateService(
    private val client: Client,
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val resourceService: AuthResourceService
) {

    fun migrateRemoteDevManager(projectConditionDTO: ProjectConditionDTO) {
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
                migrateRemoteDevManager(projectInfo)
            }
            offset += limit
        } while (migrateProjects.size == limit)
    }

    fun migrateRemoteDevManager(projectInfo: ProjectByConditionDTO) {
        logger.info("migrate remote dev manager ${projectInfo.englishName}|${projectInfo.remotedevManager}")
        try {
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
            projectInfo.remotedevManager?.let { managers ->
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
        } catch (ex: Exception) {
            logger.warn("migrate remote dev manager failed |${projectInfo.englishName}| $ex")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxMigrateService::class.java)
    }
}
