package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserAuthPermissionResource
import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.auth.service.iam.PermissionCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.pojo.Result
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
        val actionCheckPermissionMap = mutableMapOf<String, Boolean>()
        permissionBatchValidateDTO.actionList.forEach {
            // 如果action的iam挂靠资源是project，则资源类型为project,资源code为项目id
            val actionInfo = rbacCacheService.getActionInfo(it)
            val iamRelatedResourceType = actionInfo.relatedResourceType
            val checkActionPermission = permissionService.validateUserResourcePermissionByRelation(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceCode = permissionBatchValidateDTO.resourceCode,
                resourceType = iamRelatedResourceType,
                relationResourceType = null
            )
            actionCheckPermissionMap[it] = checkActionPermission
        }
        return Result(actionCheckPermissionMap)
    }
}
