package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.openapi.api.apigw.v3.ApigwLogResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwLogResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwLogResourceV3 {
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