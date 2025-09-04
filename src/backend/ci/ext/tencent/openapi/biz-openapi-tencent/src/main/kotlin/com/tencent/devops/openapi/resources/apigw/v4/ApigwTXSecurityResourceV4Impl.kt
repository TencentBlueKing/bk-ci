package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.ServiceSecurityResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTXSecurityResourceV4

@RestResource
class ApigwTXSecurityResourceV4Impl constructor(
    val client: Client
) : ApigwTXSecurityResourceV4 {
    override fun verifyProjectUser(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        credentialKey: String
    ): Result<Boolean?> {
        return client.get(ServiceSecurityResource::class).verifyProjectUser(
            projectId = projectId,
            credentialKey = credentialKey
        )
    }

    override fun getUserWaterMark(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        credentialKey: String
    ): Result<String?> {
        return client.get(ServiceSecurityResource::class).getUserWaterMark(
            projectId = projectId,
            credentialKey = credentialKey
        )
    }
}
