package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.ServiceGroupResource
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.auth.service.GroupService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGroupResourceImpl @Autowired constructor(
    val groupService: GroupService
) : ServiceGroupResource {

    override fun createGroup(
        userId: String,
        projectCode: String,
        addCreateUser: Boolean?,
        groupInfo: GroupDTO
    ): Result<String> {
        TODO("Not yet implemented")
    }
}