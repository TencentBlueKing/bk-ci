/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogPanel
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.UserLogPanelResource
import com.tencent.devops.log.service.LogPanelQueryService

@RestResource
class UserLogPanelResourceImpl(
    private val logPanelQueryService: LogPanelQueryService
) : UserLogPanelResource {

    override fun getLatestPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: String?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<QueryLogPanel> = logPanelQueryService.latest(
        userId = userId,
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        tag = tag,
        subTag = subTag,
        jobId = jobId,
        executeCount = executeCount,
        levels = levels,
        pageSize = pageSize,
        archiveFlag = archiveFlag
    )

    override fun getBeforePage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        endLineNo: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: String?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<QueryLogPanel> = logPanelQueryService.before(
        userId = userId,
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        endLineNo = endLineNo,
        tag = tag,
        subTag = subTag,
        jobId = jobId,
        executeCount = executeCount,
        levels = levels,
        pageSize = pageSize,
        archiveFlag = archiveFlag
    )

    override fun getAfterPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        startLineNo: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        levels: String?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<QueryLogPanel> = logPanelQueryService.after(
        userId = userId,
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        startLineNo = startLineNo,
        tag = tag,
        subTag = subTag,
        jobId = jobId,
        executeCount = executeCount,
        levels = levels,
        pageSize = pageSize,
        archiveFlag = archiveFlag
    )
}
