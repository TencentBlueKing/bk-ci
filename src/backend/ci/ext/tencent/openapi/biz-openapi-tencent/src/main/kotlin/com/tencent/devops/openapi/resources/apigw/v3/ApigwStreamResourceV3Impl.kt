package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.service.ServiceStreamTriggerResource
import com.tencent.devops.gitci.pojo.StreamTriggerBuildReq
import com.tencent.devops.openapi.api.apigw.v3.ApigwStreamResourceV3
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStreamResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwStreamResourceV3 {

    override fun triggerStartup(
        userId: String,
        gitProjectId: String,
        pipelineId: String,
        streamTriggerBuildReq: StreamTriggerBuildReq
    ): Result<Boolean> {
        return client.get(ServiceStreamTriggerResource::class).triggerStartup(
            userId = userId,
            projectId = "git_$gitProjectId",
            pipelineId = pipelineId,
            streamTriggerBuildReq = streamTriggerBuildReq
        )
    }
}
