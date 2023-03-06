package com.tencent.devops.auth.resources

import UserPermissionAuthResource
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPermissionAuthResourceImpl @Autowired constructor(
    val permissionService: PermissionService
) : UserPermissionAuthResource {
    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: List<String>
    ): Result<Map<String, Boolean>> {
        val actionCheckPermissionMap: MutableMap<String, Boolean> = HashMap()
        action.forEach {
            val checkActionPermission = permissionService.validateUserResourcePermissionByRelation(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                relationResourceType = null
            )
            actionCheckPermissionMap[it] = checkActionPermission
        }
        return Result(actionCheckPermissionMap)
    }

}
