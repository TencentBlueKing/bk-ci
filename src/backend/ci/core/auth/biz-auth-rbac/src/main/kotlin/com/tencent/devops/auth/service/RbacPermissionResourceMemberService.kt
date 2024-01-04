package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.project.constant.ProjectMessageCode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class RbacPermissionResourceMemberService constructor(
    private val authResourceService: AuthResourceService,
    private val iamV2ManagerService: V2ManagerService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val dslContext: DSLContext,
    private val deptService: DeptService
) : PermissionResourceMemberService {
    override fun getResourceGroupMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        group: BkAuthGroup?
    ): List<String> {
        logger.info("[RBAC-IAM] get resource group members:$projectCode|$resourceType|$resourceCode|$group")
        return when (group) {
            // 新的rbac版本中，没有ci管理员组，不可以调用此接口来获取ci管理员组的成员
            BkAuthGroup.CIADMIN, BkAuthGroup.CI_MANAGER -> emptyList()
            // 获取特定资源下全部成员
            null -> {
                getResourceGroupAndMembers(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                ).flatMap { it.userIdList }.distinct()
            }
            // 获取特定资源下特定用户组成员
            else -> {
                val dbGroupInfo = authResourceGroupDao.get(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    groupCode = group.value
                ) ?: return emptyList()
                val groupInfo = getResourceGroupAndMembers(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                ).find { it.roleId == dbGroupInfo.relationId.toInt() }
                groupInfo?.userIdList ?: emptyList()
            }
        }
    }

    override fun getResourceGroupAndMembers(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): List<BkAuthGroupAndUserList> {
        // 1、获取管理员id
        val managerId = authResourceService.get(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).relationId
        // 2、获取分级管理员下所有的用户组
        val groupInfoList = getGroupInfoList(
            resourceType = resourceType,
            managerId = managerId
        )
        logger.info(
            "[RBAC-IAM] getResourceGroupAndMembers: projectCode = $projectCode |" +
                " managerId = $managerId | groupInfoList: $groupInfoList"
        )
        // 3、获取组成员
        return groupInfoList.map { getUsersUnderGroup(groupInfo = it) }
    }

    override fun batchAddResourceGroupMembers(
        userId: String,
        projectCode: String,
        iamGroupId: Int,
        expiredTime: Long,
        members: List<String>
    ): Boolean {
        // 校验用户组是否属于该项目
        val managerId = authResourceService.get(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        ).relationId
        val isGroupBelongToProject = getGroupInfoList(
            resourceType = AuthResourceType.PROJECT.value,
            managerId = managerId
        ).map { it.id }.contains(iamGroupId)
        if (!isGroupBelongToProject) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.ERROR_GROUP_NOT_BELONG_TO_PROJECT,
                defaultMessage = "The group($iamGroupId) does not belong to the project($projectCode)!"
            )
        }
        val type = ManagerScopesEnum.getType(ManagerScopesEnum.USER)
        val pageInfoDTO = V2PageInfoDTO().apply {
            pageSize = 1000
            page = 1
        }
        val groupMemberMap = iamV2ManagerService.getRoleGroupMemberV2(
            iamGroupId,
            pageInfoDTO
        ).results.filter {
            it.type == type
        }.associateBy { it.id }
        val addMembers = mutableListOf<String>()
        // 预期的过期天数
        val expectExpiredAt = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(VALID_EXPIRED_AT)
        members.forEach {
            // 如果用户已经在用户组,并且过期时间超过30天,则不再添加
            if (groupMemberMap.containsKey(it) && groupMemberMap[it]!!.expiredAt > expectExpiredAt) {
                return@forEach
            }
            deptService.getUserInfo(
                userId = "admin",
                name = it
            ) ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.USER_NOT_EXIST,
                params = arrayOf(it),
                defaultMessage = "user $it not exist"
            )
            addMembers.add(it)
        }
        logger.info("batch add project user:$iamGroupId|$expiredTime|$addMembers")
        if (addMembers.isNotEmpty()) {
            val iamMemberInfos = addMembers.map { ManagerMember(type, it) }
            val managerMemberGroup =
                ManagerMemberGroupDTO.builder().members(iamMemberInfos).expiredAt(expiredTime).build()
            iamV2ManagerService.createRoleGroupMemberV2(iamGroupId, managerMemberGroup)
        }
        return true
    }

    private fun getGroupInfoList(
        resourceType: String,
        managerId: String
    ): List<V2ManagerRoleGroupInfo> {
        return if (resourceType == AuthResourceType.PROJECT.value) {
            val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
            permissionGradeManagerService.listGroup(
                gradeManagerId = managerId,
                searchGroupDTO = searchGroupDTO,
                page = 1,
                pageSize = 1000
            )
        } else {
            val v2PageInfoDTO = V2PageInfoDTO().apply {
                pageSize = 1000
                page = 1
            }
            iamV2ManagerService.getSubsetManagerRoleGroup(
                managerId.toInt(),
                v2PageInfoDTO
            ).results
        }
    }

    private fun getUsersUnderGroup(groupInfo: V2ManagerRoleGroupInfo): BkAuthGroupAndUserList {
        val pageInfoDTO = V2PageInfoDTO().apply {
            pageSize = 1000
            page = 1
        }
        val groupMemberInfoList = iamV2ManagerService.getRoleGroupMemberV2(groupInfo.id, pageInfoDTO).results
        logger.info(
            "[RBAC-IAM] getUsersUnderGroup ,groupId: ${groupInfo.id} | groupMemberInfoList: $groupMemberInfoList"
        )
        val members = mutableListOf<String>()
        groupMemberInfoList.forEach { memberInfo ->
            if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
                members.add(memberInfo.id)
            }
        }
        return BkAuthGroupAndUserList(
            displayName = groupInfo.name,
            roleId = groupInfo.id,
            roleName = groupInfo.name,
            userIdList = members.toSet().toList(),
            type = ""
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceMemberService::class.java)
        // 有效的过期时间,在30天内就是有效的
        private const val VALID_EXPIRED_AT = 30L
    }
}
