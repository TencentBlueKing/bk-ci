package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.auth.service.iam.impl.AbsPermissionGrantServiceImpl
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired

class BkAuthGrantPermissionServiceImpl @Autowired constructor(
    override val grantServiceImpl: GrantServiceImpl,
    override val iamConfiguration: IamConfiguration,
    override val client: Client
) : AbsPermissionGrantServiceImpl(grantServiceImpl, iamConfiguration, client) {
    override fun grantInstancePermission(projectId: String, grantInfo: GrantInstanceDTO): Boolean {
        return super.grantInstancePermission(projectId, grantInfo)
    }
}
