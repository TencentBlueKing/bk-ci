package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.store.api.atom.UserMarketAtomLogResource
import com.tencent.devops.store.service.atom.MarketAtomLogService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserMarketAtomLogResourceImpl @Autowired constructor(private val marketAtomLogService: MarketAtomLogService) :
    UserMarketAtomLogResource {

    override fun getInitLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        return marketAtomLogService.getInitLogs(userId, projectCode, pipelineId, buildId, isAnalysis, queryKeywords, tag, executeCount)
    }

    override fun getAfterLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        return marketAtomLogService.getAfterLogs(userId, projectCode, pipelineId, buildId, start, isAnalysis, queryKeywords, tag, executeCount)
    }

    override fun getMoreLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        return marketAtomLogService.getMoreLogs(userId, projectCode, pipelineId, buildId, num, fromStart, start, end, tag, executeCount)
    }
}