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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServicePipelineViewResource
import com.tencent.devops.process.constant.ProcessMessageCode.BK_VIEW_ID_AND_NAME_CANNOT_BE_EMPTY_TOGETHER
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_VIEW_NOT_FOUND_IN_PROJECT
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewId
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import javax.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineViewResourceImpl @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineViewService: PipelineViewService,
    private val pipelineViewGroupService: PipelineViewGroupService
) : ServicePipelineViewResource {

    private fun getViewId(
        viewId: String?,
        viewName: String?,
        projectId: String,
        isProject: Boolean?
    ): String {
        if (!viewId.isNullOrBlank()) return viewId
        if (viewName == null || isProject == null) throw CustomException(
            Response.Status.BAD_REQUEST,
            I18nUtil.getCodeLanMessage(BK_VIEW_ID_AND_NAME_CANNOT_BE_EMPTY_TOGETHER)
        )
        return pipelineViewService.viewName2viewId(projectId, viewName, isProject)
            ?: throw CustomException(
                Response.Status.BAD_REQUEST,
                I18nUtil.getCodeLanMessage(
                    messageCode = ERROR_VIEW_NOT_FOUND_IN_PROJECT,
                    params = arrayOf(projectId, (if (isProject) "project" else "individual"), viewName)
                )
            )
    }

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
        return Result(
            pipelineListFacadeService.listViewPipelines(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                sortType = sortType ?: PipelineSortType.CREATE_TIME,
                channelCode = ChannelCode.BS,
                viewId = getViewId(viewId, viewName, projectId, isProject),
                checkPermission = true,
                filterByPipelineName = filterByPipelineName,
                filterByCreator = filterByCreator,
                filterByLabels = filterByLabels,
                filterByViewIds = filterByViewIds,
                collation = collation ?: PipelineCollation.DEFAULT,
                showDelete = showDelete ?: false
            )
        )
    }

    override fun listView(
        userId: String,
        projectId: String,
        projected: Boolean?,
        viewType: Int?
    ): Result<List<PipelineNewViewSummary>> {
        return Result(
            pipelineViewGroupService.listView(
                userId = userId,
                projectId = projectId,
                projected = projected,
                viewType = viewType
            )
        )
    }

    override fun addView(
        userId: String,
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewId> {
        return Result(
            PipelineViewId(
                pipelineViewGroupService.addViewGroup(
                    projectId = projectId,
                    userId = userId,
                    pipelineView = pipelineView
                )
            )
        )
    }

    override fun getView(
        userId: String,
        projectId: String,
        viewId: String?,
        viewName: String?,
        isProject: Boolean?
    ): Result<PipelineNewView> {
        return Result(
            pipelineViewGroupService.getView(
                userId = userId,
                projectId = projectId,
                viewId = getViewId(viewId, viewName, projectId, isProject)
            )
        )
    }

    override fun deleteView(
        userId: String,
        projectId: String,
        viewId: String?,
        viewName: String?,
        isProject: Boolean?
    ): Result<Boolean> {
        return Result(
            pipelineViewGroupService.deleteViewGroup(
                projectId = projectId,
                userId = userId,
                viewIdEncode = getViewId(viewId, viewName, projectId, isProject)
            )
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
        return Result(
            pipelineViewGroupService.updateViewGroup(
                projectId = projectId,
                userId = userId,
                viewIdEncode = getViewId(viewId, viewName, projectId, isProject),
                pipelineView = pipelineView
            )
        )
    }
}
