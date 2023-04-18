package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserAuthPermissionResource
import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthPermissionResourceImpl @Autowired constructor(
    private val permissionResourceValidateService: PermissionResourceValidateService
) : UserAuthPermissionResource {

    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        permissionBatchValidateDTO: PermissionBatchValidateDTO
    ): Result<Map<String, Boolean>> {
        return Result(
            permissionResourceValidateService.batchValidateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                permissionBatchValidateDTO = permissionBatchValidateDTO
            )
        )
    }
}
