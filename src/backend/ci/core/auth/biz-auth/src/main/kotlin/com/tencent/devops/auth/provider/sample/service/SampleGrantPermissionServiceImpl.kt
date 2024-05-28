package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.auth.service.iam.PermissionGrantService

class SampleGrantPermissionServiceImpl : PermissionGrantService {
    override fun grantInstancePermission(projectId: String, grantInfo: GrantInstanceDTO): Boolean {
        return false
    }
}
