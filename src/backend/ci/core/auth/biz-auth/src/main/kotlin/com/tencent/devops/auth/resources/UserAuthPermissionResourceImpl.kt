package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserAuthPermissionResource
import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.auth.service.iam.PermissionCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthPermissionResourceImpl @Autowired constructor(
    val permissionService: PermissionService,
    val rbacCacheService: PermissionCacheService
) : UserAuthPermissionResource {
    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        permissionBatchValidateDTO: PermissionBatchValidateDTO
    ): Result<Map<String, Boolean>> {

        val projectActionList = mutableSetOf<String>()
        val resourceActionList = mutableSetOf<String>()

        permissionBatchValidateDTO.actionList.forEach { action ->
            val actionInfo = rbacCacheService.getActionInfo(action)
            val iamRelatedResourceType = actionInfo.relatedResourceType
            if (iamRelatedResourceType == AuthResourceType.PROJECT.value) {
                projectActionList.add(action)
            } else {
                resourceActionList.add(action)
            }
        }

        val projectActionPermissions = permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = projectActionList.toList(),
            projectCode = projectCode,
            resourceCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
        )

        val resourceActionPermissions = permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = resourceActionList.toList(),
            projectCode = projectCode,
            resourceCode = permissionBatchValidateDTO.resourceCode,
            resourceType = permissionBatchValidateDTO.resourceType
        )

        val actionCheckPermissionMap = mutableMapOf<String, Boolean>()
        actionCheckPermissionMap.putAll(projectActionPermissions)
        actionCheckPermissionMap.putAll(resourceActionPermissions)
        return Result(actionCheckPermissionMap)
    }
}
