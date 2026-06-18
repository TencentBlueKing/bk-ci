package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTXPipelineResourceV4
import com.tencent.devops.process.api.service.ServiceTXPipelineResource
import com.tencent.devops.process.pojo.PipelineExportV2YamlData
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXPipelineResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwTXPipelineResourceV4 {
    override fun exportPipelineGitCI(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<PipelineExportV2YamlData> {
        return client.get(ServiceTXPipelineResource::class).exportPipelineGitCI(userId, projectId, pipelineId)
    }

    override fun listVisiblePipelines(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        targetUserId: String,
        pipelineName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<SimplePipeline>> {
        return client.get(ServiceTXPipelineResource::class).listVisiblePipelines(
            userId = userId,
            projectId = projectId,
            targetUserId = targetUserId,
            pipelineName = pipelineName,
            page = page,
            pageSize = pageSize
        )
    }
}
