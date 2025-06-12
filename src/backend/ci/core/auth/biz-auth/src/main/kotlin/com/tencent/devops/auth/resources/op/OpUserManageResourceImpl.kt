package com.tencent.devops.auth.resources.op

import com.tencent.devops.auth.api.op.OpUserManageResource
import com.tencent.devops.auth.service.UserManageService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class OpUserManageResourceImpl(
    private val userManageService: UserManageService
) : OpUserManageResource {
    override fun syncUserInfoData(): Result<Boolean> {
        userManageService.syncUserInfoData()
        return Result(true)
    }

    override fun syncDepartmentInfoData(): Result<Boolean> {
        userManageService.syncDepartmentInfoData()
        return Result(true)
    }

    override fun syncDepartmentRelations(): Result<Boolean> {
        userManageService.syncDepartmentRelations()
        return Result(true)
    }
}
