package com.tencent.devops.auth.service.gitci

import com.tencent.devops.auth.service.PermissionProjectService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import org.springframework.beans.factory.annotation.Autowired

class GitCIPermissionProjectServiceImpl @Autowired constructor(

): PermissionProjectService {

    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun getProjectUsers(serviceCode: String, projectCode: String, group: BkAuthGroup?): List<String> {
        return emptyList()
    }

    override fun getProjectGroupAndUserList(serviceCode: String, projectCode: String): List<BkAuthGroupAndUserList> {
        return emptyList()
    }

    override fun getUserProjects(userId: String): List<String> {
        return emptyList()
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return true
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }
}
