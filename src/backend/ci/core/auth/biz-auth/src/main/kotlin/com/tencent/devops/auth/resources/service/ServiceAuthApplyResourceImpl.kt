package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceAuthApplyResource
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAuthApplyResourceImpl @Autowired constructor(
    val permissionApplyService: PermissionApplyService
) : ServiceAuthApplyResource {
    override fun applyToJoinGroup(userId: String, applyJoinGroupInfo: ApplyJoinGroupInfo): Result<Boolean> {
        return Result(
            permissionApplyService.applyToJoinGroup(
                userId = userId,
                applyJoinGroupInfo = applyJoinGroupInfo
            )
        )
    }

    override fun getRedirectInformation(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String?
    ): Result<AuthApplyRedirectInfoVo> {
        return Result(
            permissionApplyService.getRedirectInformation(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )
        )
    }
}
