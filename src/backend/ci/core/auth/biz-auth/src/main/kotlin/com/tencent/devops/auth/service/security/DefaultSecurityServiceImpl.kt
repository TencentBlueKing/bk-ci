package com.tencent.devops.auth.service.security

import com.tencent.devops.auth.pojo.vo.SecOpsWaterMarkInfoVo
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionProjectService

class DefaultSecurityServiceImpl constructor(
    deptService: DeptService,
    permissionProjectService: PermissionProjectService
) : SecurityService(deptService, permissionProjectService) {
    override fun getUserWaterMark(userId: String): SecOpsWaterMarkInfoVo {
        return SecOpsWaterMarkInfoVo(
            type = "",
            data = ""
        )
    }
}
