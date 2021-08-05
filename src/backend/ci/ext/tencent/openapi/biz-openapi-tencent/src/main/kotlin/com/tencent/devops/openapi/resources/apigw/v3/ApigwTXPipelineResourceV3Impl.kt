package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwTXPipelineResourceV3
import com.tencent.devops.process.api.service.ServiceTXPipelineResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXPipelineResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwTXPipelineResourceV3 {
    override fun exportPipelineGitCI(userId: String, projectId: String, pipelineId: String): String {
        return client.get(ServiceTXPipelineResource::class).exportPipelineGitCI(userId, projectId, pipelineId)
    }
}
