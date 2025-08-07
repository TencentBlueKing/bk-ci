package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.service.ServiceUserApiAuthPermissionResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceUserApiAuthPermissionResourceImpl @Autowired constructor(
    private val permissionProjectService: PermissionProjectService
) : ServiceUserApiAuthPermissionResource {
    override fun checkVisitPermission(
        userId: String,
        projectId: String
    ): Result<Boolean> {
        return Result(
            permissionProjectService.isProjectMember(
                userId = userId,
                projectCode = projectId,
            )
        )
    }
}
