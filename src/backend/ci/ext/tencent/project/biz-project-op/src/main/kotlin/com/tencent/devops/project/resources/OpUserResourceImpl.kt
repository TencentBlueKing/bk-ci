package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpUserResource
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectUserRefreshService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpUserResourceImpl @Autowired constructor(
    val projectUserRefreshService: ProjectUserRefreshService
) : OpUserResource {

    override fun refreshUserGroup(userId: String): Result<UserDeptDetail?> {
        return Result(projectUserRefreshService.refreshUser(userId))
    }
}