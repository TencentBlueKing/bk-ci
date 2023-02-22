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

package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwPipelineViewResourceV4
import com.tencent.devops.process.api.service.ServicePipelineViewResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewId
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineViewResourceV4Impl @Autowired constructor(private val client: Client) :
    ApigwPipelineViewResourceV4 {

    override fun listViewPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        filterByViewIds: String?,
        viewId: String?,
        viewName: String?,
        isProject: Boolean?,
        collation: PipelineCollation?,
        showDelete: Boolean?
    ): Result<PipelineViewPipelinePage<Pipeline>> {
        logger.info(
            "OPENAPI_PIPELINE_VIEW_V4|$userId|list pipelines|" +
                "$projectId|$page|$pageSize|$sortType|$filterByPipelineName|$filterByCreator|" +
                "$filterByLabels|$filterByViewIds|$viewId|$viewName|$isProject|$collation|$showDelete|"
        )
        return client.get(ServicePipelineViewResource::class).listViewPipelines(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            sortType = sortType,
            filterByPipelineName = filterByPipelineName,
            filterByCreator = filterByCreator,
            filterByLabels = filterByLabels,
            filterByViewIds = filterByViewIds,
            viewId = viewId,
            viewName = viewName,
            isProject = isProject,
            collation = collation
        )
    }

    override fun listView(
        userId: String,
        projectId: String,
        projected: Boolean?,
        viewType: Int?
    ): Result<List<PipelineNewViewSummary>> {
        logger.info("OPENAPI_PIPELINE_VIEW_V4|$userId|list view|$projectId|$projected|$viewType")
        return client.get(ServicePipelineViewResource::class).listView(
            userId = userId,
            projectId = projectId,
            projected = projected,
            viewType = viewType
        )
    }

    override fun addView(
        userId: String,
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewId> {
        logger.info("OPENAPI_PIPELINE_VIEW_V4|$userId|add view|$projectId|$pipelineView")
        return client.get(ServicePipelineViewResource::class).addView(
            userId = userId,
            projectId = projectId,
            pipelineView = pipelineView
        )
    }

    override fun getView(
        userId: String,
        projectId: String,
        viewId: String?,
        viewName: String?,
        isProject: Boolean?
    ): Result<PipelineNewView> {
        logger.info("OPENAPI_PIPELINE_VIEW_V4|$userId|get view|$projectId|$viewId|$viewName|$isProject")
        return client.get(ServicePipelineViewResource::class).getView(
            userId = userId,
            projectId = projectId,
            viewId = viewId,
            viewName = viewName,
            isProject = isProject
        )
    }

    override fun deleteView(
        userId: String,
        projectId: String,
        viewId: String?,
        viewName: String?,
        isProject: Boolean?
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_VIEW_V4|$userId|delete view|$projectId|$viewId|$viewName|$isProject")
        return client.get(ServicePipelineViewResource::class).deleteView(
            userId = userId,
            projectId = projectId,
            viewId = viewId,
            viewName = viewName,
            isProject = isProject
        )
    }

    override fun updateView(
        userId: String,
        projectId: String,
        viewId: String?,
        viewName: String?,
        isProject: Boolean?,
        pipelineView: PipelineViewForm
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_VIEW_V4|$userId|update view|$projectId|$viewId|$viewName|$isProject")
        return client.get(ServicePipelineViewResource::class).updateView(
            userId = userId,
            projectId = projectId,
            viewId = viewId,
            viewName = viewName,
            isProject = isProject,
            pipelineView = pipelineView
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineViewResourceV4Impl::class.java)
    }
}
