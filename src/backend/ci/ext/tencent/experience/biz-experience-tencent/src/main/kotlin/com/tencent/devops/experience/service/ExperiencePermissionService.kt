package com.tencent.devops.experience.service

import com.tencent.devops.common.auth.api.AuthPermission

interface ExperiencePermissionService {
    fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    )

    fun createTaskResource(user: String, projectId: String, experienceId: Long, experienceName: String)

    fun filterExperience(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>>

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
}
