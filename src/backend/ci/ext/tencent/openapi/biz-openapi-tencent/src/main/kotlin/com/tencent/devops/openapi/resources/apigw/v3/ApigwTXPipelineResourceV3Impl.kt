package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwTXPipelineResourceV3
import com.tencent.devops.process.api.service.ServiceTXPipelineResource
import com.tencent.devops.process.pojo.PipelineExportV2YamlData
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXPipelineResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwTXPipelineResourceV3 {
    override fun exportPipelineGitCI(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<PipelineExportV2YamlData> {
        return client.get(ServiceTXPipelineResource::class).exportPipelineGitCI(userId, projectId, pipelineId)
    }
}
