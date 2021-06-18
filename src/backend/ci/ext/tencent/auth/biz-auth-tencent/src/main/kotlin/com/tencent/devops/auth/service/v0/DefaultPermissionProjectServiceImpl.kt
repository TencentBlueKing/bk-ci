package com.tencent.devops.auth.service.v0

import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import org.springframework.beans.factory.annotation.Autowired

class DefaultPermissionProjectServiceImpl @Autowired constructor(
    private val authProjectApi: AuthProjectApi,
    val authServiceCode: BSCommonAuthServiceCode
) : PermissionProjectService {

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {

        return authProjectApi.getProjectUsers(
            serviceCode = authServiceCode,
            projectCode = projectCode,
            group = group
        )
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        return authProjectApi.getProjectGroupAndUserList(
            serviceCode = authServiceCode,
            projectCode = projectCode
        )
    }

    override fun getUserProjects(userId: String): List<String> {
        return authProjectApi.getUserProjects(
            serviceCode = authServiceCode,
            userId = userId,
            supplier = null
        )
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return authProjectApi.isProjectUser(
            user = userId,
            projectCode = projectCode,
            group = group,
            serviceCode = authServiceCode
        )
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return authProjectApi.getProjectRoles(
            serviceCode = authServiceCode,
            projectCode = projectCode,
            projectId = projectId
        )
    }
}
