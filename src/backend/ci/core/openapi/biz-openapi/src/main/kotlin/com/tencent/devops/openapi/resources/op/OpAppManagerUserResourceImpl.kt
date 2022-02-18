package com.tencent.devops.openapi.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.op.OpAppManagerUserResource
import com.tencent.devops.openapi.pojo.AppManagerInfo
import com.tencent.devops.openapi.service.op.AppUserInfoService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAppManagerUserResourceImpl @Autowired constructor(
    val appUserInfoService: AppUserInfoService
) : OpAppManagerUserResource {
    override fun setGroup(userName: String, appCode: String, appManagerInfo: AppManagerInfo): Result<Boolean> {
        return Result(appUserInfoService.bindAppManagerUser(userName, appManagerInfo))
    }

    override fun getGroup(appCode: String): Result<String?> {
        return Result(appUserInfoService.get(appCode))
    }

    override fun deleteProject(userName: String, id: Int): Result<Boolean> {
        return Result(appUserInfoService.delete(id))
    }
}
