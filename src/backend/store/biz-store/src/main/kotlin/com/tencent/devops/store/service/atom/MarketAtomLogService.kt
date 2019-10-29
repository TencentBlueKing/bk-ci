package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.log.model.pojo.QueryLogs

/**
 * 插件市场-插件日志业务逻辑类
 * since: 2019-08-15
 */
interface MarketAtomLogService {

    fun getInitLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?>

    fun getAfterLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?>

    fun getMoreLogs(
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
    ): Result<QueryLogs?>
}
