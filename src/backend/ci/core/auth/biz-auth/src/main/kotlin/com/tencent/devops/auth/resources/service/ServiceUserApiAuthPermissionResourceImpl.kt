package com.tencent.devops.auth.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.service.ServiceUserApiAuthPermissionResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceUserApiAuthPermissionResourceImpl @Autowired constructor(
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode
) : ServiceUserApiAuthPermissionResource {
    override fun checkVisitPermission(
        userId: String,
        projectId: String
    ): Result<Boolean> {
        return Result(
            authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = projectAuthServiceCode,
                resourceType = AuthResourceType.PROJECT,
                projectCode = projectId,
                permission = AuthPermission.VISIT
            )
        )
    }
}
