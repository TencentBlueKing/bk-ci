package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.vo.ProjectPermissionInfoVO
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList

class SampleAuthPermissionProjectService : PermissionProjectService {
    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        return emptyList()
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        return emptyList()
    }

    override fun getUserProjects(userId: String): List<String> {
        return emptyList()
    }

    override fun getUserProjectsByPermission(
        userId: String,
        action: String,
        resourceType: String?
    ): List<String> {
        return emptyList()
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return true
    }

    override fun isProjectMember(userId: String, projectCode: String): Boolean {
        return true
    }

    override fun checkUserInProjectLevelGroup(userId: String, projectCode: String): Boolean {
        return false
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return true
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        return true
    }

    override fun batchCreateProjectUser(
        userId: String,
        projectCode: String,
        roleCode: String,
        members: List<String>
    ): Boolean = true

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }

    override fun getProjectPermissionInfo(projectCode: String): ProjectPermissionInfoVO {
        return ProjectPermissionInfoVO("", "", "", emptyList(), emptyList())
    }
}
