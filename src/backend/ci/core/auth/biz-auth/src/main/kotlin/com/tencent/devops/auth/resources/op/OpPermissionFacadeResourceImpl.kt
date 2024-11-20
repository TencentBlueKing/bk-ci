package com.tencent.devops.auth.resources.op

import com.tencent.devops.auth.api.op.OpPermissionFacadeResource
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class OpPermissionFacadeResourceImpl(
    private val resourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val resourceGroupService: PermissionResourceGroupService
) : OpPermissionFacadeResource {
    override fun createCustomGroupAndPermissions(
        projectId: String,
        customGroupCreateReq: CustomGroupCreateReq
    ): Result<Int> {
        return Result(
            data = resourceGroupService.createCustomGroupAndPermissions(
                projectId = projectId,
                customGroupCreateReq = customGroupCreateReq
            )
        )
    }

    override fun grantAllProjectGroupsPermission(
        projectId: String,
        projectName: String,
        actions: List<String>
    ): Result<Boolean> {
        return Result(
            resourceGroupPermissionService.grantAllProjectGroupsPermission(
                projectCode = projectId,
                projectName = projectName,
                actions = actions
            )
        )
    }
}
