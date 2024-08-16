package com.tencent.devops.experience.permission

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.GroupDao
import com.tencent.devops.experience.service.ExperiencePermissionService
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.DSLContext

class MockExperiencePermissionService(
    private val dslContext: DSLContext,
    private val groupDao: GroupDao,
    private val experienceDao: ExperienceDao
) : ExperiencePermissionService {

    override fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    ) = Unit

    override fun validateCreateTaskPermission(
        user: String,
        projectId: String
    ) = true

    override fun validateDeleteExperience(
        experienceId: Long,
        userId: String,
        projectId: String,
        message: String
    ) = Unit

    override fun createTaskResource(
        user: String,
        projectId: String,
        experienceId: Long,
        experienceName: String
    ) = Unit

    override fun filterExperience(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val experienceIds = experienceDao.list(dslContext, projectId, null, null).map { it.id }
        val experienceMap = mutableMapOf<AuthPermission, List<Long>>()
        authPermissions.forEach {
            experienceMap[it] = experienceIds
        }
        return experienceMap
    }

    override fun filterCanListExperience(
        user: String,
        projectId: String,
        experienceRecordList: List<TExperienceRecord>
    ) = experienceRecordList

    override fun validateCreateGroupPermission(user: String, projectId: String) = true

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) = Unit

    override fun createGroupResource(
        userId: String,
        projectId: String,
        groupId: Long,
        groupName: String
    ) = Unit

    override fun modifyGroupResource(
        projectId: String,
        groupId: Long,
        groupName: String
    ) = Unit

    override fun deleteGroupResource(projectId: String, groupId: Long) = Unit

    override fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val groupIds = groupDao.list(
            dslContext = dslContext,
            projectId = projectId,
            offset = 0,
            limit = 1000).map { it.id }
        val experienceMap = mutableMapOf<AuthPermission, List<Long>>()
        authPermissions.forEach {
            experienceMap[it] = groupIds
        }
        return experienceMap
    }

    override fun filterCanListGroup(
        user: String,
        projectId: String,
        groupRecordIds: List<Long>
    ) = groupRecordIds
}
