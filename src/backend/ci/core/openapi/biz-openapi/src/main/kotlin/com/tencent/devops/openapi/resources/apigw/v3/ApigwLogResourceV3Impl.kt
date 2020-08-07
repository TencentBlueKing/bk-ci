package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.openapi.api.apigw.v3.ApigwLogResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

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
                "elementId[$elementId] jobId[$jobId] executeCount[$executeCount] isAnalysis[$isAnalysis] jobId[$jobId]"
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

    override fun getMoreLogs(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        logger.info(
            "getMoreLogs project[$projectId] pipelineId[$pipelineId] buildId[$buildId] num[$num] " +
                "jobId[$jobId] executeCount[$executeCount] fromStart[$fromStart]  start[$start] end[$end] tag[$tag] jobId[$jobId]"
        )
        return client.get(ServiceLogResource::class).getMoreLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            num = num,
            fromStart = fromStart,
            start = start,
            end = end,
            tag = tag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun getAfterLogs(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        logger.info(
            "getAfterLogs project[$projectId] pipelineId[$pipelineId] buildId[$buildId] isAnalysis[$isAnalysis] queryKeywords[$queryKeywords]" +
                "jobId[$jobId] executeCount[$executeCount]  start[$start] tag[$tag] jobId[$jobId]"
        )
        return client.get(ServiceLogResource::class).getAfterLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            start = start,
            isAnalysis = isAnalysis,
            queryKeywords = queryKeywords,
            tag = tag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun downloadLogs(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Response {
        logger.info(
            "downloadLogs project[$projectId] pipelineId[$pipelineId] buildId[$buildId]" +
                "jobId[$jobId] executeCount[$executeCount] tag[$tag] jobId[$jobId]"
        )
        return client.get(ServiceLogResource::class).downloadLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = tag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}