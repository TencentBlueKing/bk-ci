package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO

interface PermissionGrantService {
    fun grantInstancePermission(
        projectId: String,
        grantInfo: GrantInstanceDTO
    ): Boolean
}
