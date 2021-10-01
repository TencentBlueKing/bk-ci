package com.tencent.devops.auth.service.iam

interface PermissionGrantService {
    fun grantInstancePermission(
        userId: String,
        action: String,
        projectId: String,
        resourceCode: String,
        resourceType: String
    ): Boolean
}
