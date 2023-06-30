package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType.Companion.getDisplayName
import com.tencent.devops.common.web.utils.I18nUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbsPermissionRoleServiceImpl @Autowired constructor(
    private val groupService: AuthGroupService
) : PermissionRoleService {
    override fun createPermissionRole(
        userId: String,
        projectId: Int,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    ): Int {
        var groupType = groupInfo.defaultGroup
        var groupName = ""
        var displayName = ""
        if (!DefaultGroupType.contains(groupInfo.code)) {
            groupType = false
            groupName = groupInfo.name ?: ""
            displayName = groupInfo.displayName ?: groupInfo.name
        } else {
            groupType = true
            groupName = groupInfo.name
            displayName = DefaultGroupType.get(groupInfo.code).getDisplayName(I18nUtil.getLanguage(userId))
        }
        val roleId = groupService.createGroup(
            userId = userId,
            projectCode = projectCode,
            groupInfo = GroupDTO(
                groupCode = groupInfo.code,
                groupType = groupType,
                groupName = groupName,
                displayName = displayName,
                relationId = null
            )
        )
        try {
            groupCreateExt(
                roleId = roleId,
                userId = userId,
                projectId = projectId,
                projectCode = projectCode,
                groupInfo = groupInfo
            )
            logger.info("create ext group success : projectCode = $projectCode | roleId = $roleId")
        } catch (iamException: IamException) {
            logger.warn("create Role ext fail : iamException = $iamException")
            groupService.deleteGroup(roleId, false)
            throw RemoteServiceException("create project role fail: ${iamException.errorMsg}")
        } catch (e: Exception) {
            logger.warn("create Role ext fail : $e")
            groupService.deleteGroup(roleId, false)
            throw ParamBlankException("create project role fail")
        }
        return roleId
    }

    override fun renamePermissionRole(
        userId: String,
        projectId: Int,
        roleId: Int,
        groupInfo: ProjectRoleDTO
    ) {
        groupService.updateGroupName(userId, roleId, groupInfo)
        val iamId = groupService.getRelationId(roleId) ?: return
        // 若没有关联id, 无需修改关联系统信息
        renameRoleExt(
            userId = userId,
            projectId = projectId,
            roleId = iamId!!.toInt(),
            groupInfo = groupInfo
        )
    }

    override fun deletePermissionRole(userId: String, projectId: Int, roleId: Int) {
        val iamId = groupService.getRelationId(roleId)
        if (iamId != null) {
            // 优先删除扩展系统内的数据,最后再删本地数据
            deleteRoleExt(userId, projectId, iamId.toInt())
        }
        groupService.deleteGroup(roleId)
    }

    abstract fun groupCreateExt(
        roleId: Int,
        userId: String,
        projectId: Int,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    )

    abstract fun renameRoleExt(
        userId: String,
        projectId: Int,
        roleId: Int,
        groupInfo: ProjectRoleDTO
    )

    abstract fun deleteRoleExt(
        userId: String,
        projectId: Int,
        roleId: Int
    )

    companion object {
        private val logger = LoggerFactory.getLogger(AbsPermissionRoleServiceImpl::class.java)
    }
}
