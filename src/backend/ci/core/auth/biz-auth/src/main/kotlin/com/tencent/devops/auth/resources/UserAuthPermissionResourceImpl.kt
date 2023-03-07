package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserAuthPermissionResource
import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthPermissionResourceImpl @Autowired constructor(
    val permissionService: PermissionService
) : UserAuthPermissionResource {
    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        permissionBatchValidateDTO: PermissionBatchValidateDTO
    ): Result<Map<String, Boolean>> {
        val actionCheckPermissionMap = mutableMapOf<String, Boolean>()
        permissionBatchValidateDTO.actionList.forEach {
            val checkActionPermission = permissionService.validateUserResourcePermissionByRelation(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceCode = permissionBatchValidateDTO.resourceCode,
                resourceType = permissionBatchValidateDTO.resourceType,
                relationResourceType = null
            )
            actionCheckPermissionMap[it] = checkActionPermission
        }
        return Result(actionCheckPermissionMap)
    }
}
