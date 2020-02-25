package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.openapi.api.v2.ApigwLogResourceV2
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwLogResourceV2Impl @Autowired constructor(
    private val client: Client
): ApigwLogResourceV2 {
    override fun getInitLogs(
        projectId: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        elementId: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        return client.get(ServiceLogResource::class).getInitLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = elementId,
            jobId = jobId,
            executeCount = executeCount,
            isAnalysis = isAnalysis,
            queryKeywords = queryKeywords
        )
    }
}