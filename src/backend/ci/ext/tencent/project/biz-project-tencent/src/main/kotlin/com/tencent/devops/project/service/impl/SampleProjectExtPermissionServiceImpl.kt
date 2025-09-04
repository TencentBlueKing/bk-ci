package com.tencent.devops.project.service.impl

import com.tencent.devops.project.service.ProjectExtPermissionService

class SampleProjectExtPermissionServiceImpl : ProjectExtPermissionService {
    override fun verifyUserProjectPermission(
        accessToken: String,
        projectCode: String,
        userId: String
    ) = false

    override fun createUser2Project(
        createUser: String,
        userIds: List<String>,
        projectCode: String,
        roleId: Int?,
        roleName: String?,
        checkManager: Boolean
    ) = false

    override fun grantInstancePermission(
        userId: String,
        projectId: String,
        action: String,
        resourceType: String,
        resourceCode: String,
        userList: List<String>
    ) = false
}
