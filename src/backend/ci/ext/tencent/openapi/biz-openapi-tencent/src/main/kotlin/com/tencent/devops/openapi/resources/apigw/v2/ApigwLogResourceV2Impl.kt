package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.openapi.api.apigw.v2.ApigwLogResourceV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwLogResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwLogResourceV2 {
    override fun getInitLogs(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        elementId: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        logger.info(
            "getInitLogs project[$projectId] pipelineId[$pipelineId] buildId[$buildId] queryKeywords[$queryKeywords] " +
                "elementId[$elementId] jobId[$jobId]"
        )
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

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}