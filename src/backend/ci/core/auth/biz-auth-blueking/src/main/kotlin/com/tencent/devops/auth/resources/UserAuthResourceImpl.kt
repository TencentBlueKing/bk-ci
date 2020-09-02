package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.UserAuthResource
import com.tencent.devops.auth.pojo.PermissionUrlDTO
import com.tencent.devops.auth.service.IamService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthResourceImpl @Autowired constructor(
    val iamService: IamService
) : UserAuthResource {
    override fun permissionUrl(permissionUrlDTO: List<PermissionUrlDTO>): Result<String?> {
        return iamService.getPermissionUrl(permissionUrlDTO)
    }
}