package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceCmdbNodeResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwTXEnvironmentResourceV3
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXEnvironmentResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwTXEnvironmentResourceV3 {
    override fun addCmdbNodes(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeIps: List<String>
    ): Result<Boolean> {
        return client.get(ServiceCmdbNodeResource::class).addCmdbNodes(userId, projectId, nodeIps)
    }
}
