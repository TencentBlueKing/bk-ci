/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.view.PipelineViewService
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_LIST_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import com.tencent.devops.stream.api.service.ServiceGitForAppResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class AppPipelineViewResourceImpl @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineViewService: PipelineViewService,
    private val client: Client,
    private val bkTag: BkTag
) : AppPipelineViewResource {

    @Value("\${gitCI.tag:#{null}}")
    private val gitCI: String? = null

    override fun listViewPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        viewId: String,
        filterInvalid: Boolean?
    ): Result<PipelineViewPipelinePage<Pipeline>> {
        return Result(
            pipelineListFacadeService.listViewPipelines(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                sortType = sortType ?: PipelineSortType.CREATE_TIME,
                channelCode = ChannelCode.BS,
                viewId = viewId,
                checkPermission = true,
                filterByPipelineName = filterByPipelineName,
                filterByCreator = filterByCreator,
                filterByLabels = filterByLabels,
                filterInvalid = filterInvalid ?: true
            )
        )
    }

    override fun listViewPipelinesV2(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        viewId: String,
        filterInvalid: Boolean?
    ): Result<Pagination<Pipeline>> {
        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS

        // 兼容我的流水线
        val finalViewId = if (viewId == PIPELINE_VIEW_MY_PIPELINES) {
            PIPELINE_VIEW_MY_LIST_PIPELINES
        } else viewId

        val listViewPipelines = pipelineListFacadeService.listViewPipelines(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            sortType = sortType ?: PipelineSortType.CREATE_TIME,
            channelCode = channelCode,
            viewId = finalViewId,
            checkPermission = true,
            filterByPipelineName = if (channelCode == ChannelCode.GIT) null else filterByPipelineName,
            filterByCreator = filterByCreator, filterByLabels = filterByLabels,
            filterInvalid = filterInvalid ?: true
        )

        // gitci 返回值兼容
        val records = if (channelCode == ChannelCode.GIT) {
            val gitciPipelines = bkTag.invokeByTag(gitCI) {
                client.get(ServiceGitForAppResource::class).getGitCIPipelines(
                    projectId = projectId,
                    page = 1,
                    pageSize = 10000,
                    sortType = sortType,
                    search = filterByPipelineName
                )
            }.data?.records?.associate { it.pipelineId to it.displayName } ?: emptyMap()

            listViewPipelines.records.filter { gitciPipelines.containsKey(it.pipelineId) }.onEach {
                gitciPipelines[it.pipelineId]?.let { display -> it.pipelineName = display }
            }.toList()
        } else {
            listViewPipelines.records
        }

        val hasNext = listViewPipelines.count > listViewPipelines.page * listViewPipelines.pageSize

        return Result(Pagination(hasNext, records))
    }

    override fun getViewSettings(userId: String, projectId: String): Result<PipelineViewSettings> {
        return Result(pipelineViewService.getViewSettings(userId, projectId))
    }
}
