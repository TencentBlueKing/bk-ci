package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.ServiceUserGroupResource
import com.tencent.devops.auth.service.GroupUserService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceUserGroupResourceImpl @Autowired constructor(
    val groupUserService: GroupUserService
) : ServiceUserGroupResource {

    override fun addUser2Group(userId: String, groupId: String): Result<Boolean> {
        return groupUserService.addUser2Group(userId, groupId)
    }

}