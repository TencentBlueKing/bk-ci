package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.devops.auth.service.iam.impl.AbsPermissionGrantServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BkAuthGrantPermissionServiceImpl @Autowired constructor(
    override val grantServiceImpl: GrantServiceImpl,
    override val iamConfiguration: IamConfiguration
) : AbsPermissionGrantServiceImpl(grantServiceImpl, iamConfiguration) {
    override fun grantInstancePermission(
        userId: String,
        action: String,
        projectId: String,
        resourceCode: String,
        resourceType: String
    ): Boolean {
        return super.grantInstancePermission(userId, action, projectId, resourceCode, resourceType)
    }
}
