package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.service.ServiceUserApiAuthPermissionResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceUserApiAuthPermissionResourceImpl @Autowired constructor(
    private val permissionService: PermissionService,
) : ServiceUserApiAuthPermissionResource {
    override fun checkVisitPermission(
        userId: String,
        projectId: String
    ): Result<Boolean> {
        val action = RbacAuthUtils.buildAction(
            authResourceType = AuthResourceType.PROJECT,
            authPermission = AuthPermission.VISIT
        )
        return Result(
            permissionService.validateUserResourcePermission(
                userId = userId,
                action = action,
                projectCode = projectId,
                resourceType = AuthResourceType.PROJECT.value
            )
        )
    }
}
