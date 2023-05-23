package com.tencent.devops.experience.service

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.model.experience.tables.records.TExperienceRecord

interface ExperiencePermissionService {
    fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    )

    // 校验是否有项目下创建版本体验的权限，只有rbac需要校验，其他的默认返回true
    fun validateCreateTaskPermission(
        user: String,
        projectId: String
    ): Boolean

    fun validateDeleteExperience(
        experienceId: Long,
        userId: String,
        projectId: String,
        message: String
    )

    fun createTaskResource(
        user: String,
        projectId: String,
        experienceId: Long,
        experienceName: String
    )

    fun filterExperience(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>>

    fun filterCanListExperience(
        user: String,
        projectId: String,
        experienceRecordList: List<TExperienceRecord>
    ): List<TExperienceRecord>

    // 校验是否有项目下创建版本体验用户组的权限，只有rbac需要校验，其他的默认返回true
    fun validateCreateGroupPermission(
        user: String,
        projectId: String
    ): Boolean

    fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    )

    fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String)

    fun modifyGroupResource(projectId: String, groupId: Long, groupName: String)

    fun deleteGroupResource(projectId: String, groupId: Long)

    fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>>

    fun filterCanListGroup(
        user: String,
        projectId: String,
        groupRecordIds: List<Long>
    ): List<Long>
}
