package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.service.UserProjectPermissionService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.service.ServiceUserProjectMemberPermissionResource
import org.springframework.beans.factory.annotation.Autowired


@RestResource
class ServiceUserProjectMemberPermissionResourceImpl @Autowired constructor(
    private val userProjectPermissionService: UserProjectPermissionService
) : ServiceUserProjectMemberPermissionResource {

    override fun checkMember(userId: String, projectId: String): Result<Boolean> {
        return Result(userProjectPermissionService.checkMember(projectId, userId))
    }
}