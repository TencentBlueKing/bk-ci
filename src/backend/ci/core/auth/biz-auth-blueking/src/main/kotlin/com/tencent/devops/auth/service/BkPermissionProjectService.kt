package com.tencent.devops.auth.service

import com.tencent.devops.auth.service.iam.impl.AbsPermissionProjectService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList

class BkPermissionProjectService : AbsPermissionProjectService() {
    override fun getProjectUsers(serviceCode: String, projectCode: String, group: BkAuthGroup?): List<String> {
        return super.getProjectUsers(serviceCode, projectCode, group)
    }

    override fun getProjectGroupAndUserList(serviceCode: String, projectCode: String): List<BkAuthGroupAndUserList> {
        return super.getProjectGroupAndUserList(serviceCode, projectCode)
    }

    override fun getUserProjects(userId: String): List<String> {
        return super.getUserProjects(userId)
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return super.isProjectUser(userId, projectCode, group)
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return super.createProjectUser(userId, projectCode, role)
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return super.getProjectRoles(projectCode, projectId)
    }
}
