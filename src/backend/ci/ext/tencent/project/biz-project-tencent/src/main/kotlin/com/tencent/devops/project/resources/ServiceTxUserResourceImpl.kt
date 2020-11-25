package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTxUserResourceImpl @Autowired constructor(
    val projectServiceCode: BSProjectServiceCodec,
    val bkAuthProjectApi: BSAuthProjectApi
) : ServiceTxUserResource {
    override fun getProjectUserRoles(projectCode: String, roleId: BkAuthGroup): Result<List<String>> {
        return Result(bkAuthProjectApi.getProjectUsers(projectServiceCode, projectCode, roleId))
    }
}