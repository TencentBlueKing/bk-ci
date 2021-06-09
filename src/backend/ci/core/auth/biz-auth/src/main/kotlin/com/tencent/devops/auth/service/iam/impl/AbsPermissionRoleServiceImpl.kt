package com.tencent.devops.auth.service.iam.impl

import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.iam.PermissionRoleService
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
        val roleId = groupService.createGroup(
            userId = userId,
            projectCode = projectCode,
            groupInfo = GroupDTO(
                groupCode = groupInfo.code,
                groupType = groupInfo.type,
                groupName = groupInfo.name,
                displayName = groupInfo.name,
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
        } catch (e: Exception) {
            logger.warn("create Role ext fail $e")
            groupService.deleteGroup(roleId, false)
        }
        return roleId
    }

    override fun renamePermissionRole(userId: String, projectId: Int, roleId: Int, groupInfo: ProjectRoleDTO) {
        renameRoleExt(
            userId = userId,
            projectId = projectId,
            roleId = roleId,
            groupInfo = groupInfo
        )
    }

    override fun deletePermissionRole(userId: String, projectId: Int, roleId: Int) {
        // 优先删除扩展系统内的数据,最后再删本地数据
        deleteRoleExt(userId, projectId, roleId)
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
        val logger = LoggerFactory.getLogger(AbsPermissionRoleServiceImpl::class.java)
    }
}
