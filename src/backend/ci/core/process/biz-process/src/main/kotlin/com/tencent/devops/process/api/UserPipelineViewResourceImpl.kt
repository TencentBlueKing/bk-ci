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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineViewResource
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewBulkAdd
import com.tencent.devops.process.pojo.classify.PipelineViewBulkRemove
import com.tencent.devops.process.pojo.classify.PipelineViewDict
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewHitFilters
import com.tencent.devops.process.pojo.classify.PipelineViewId
import com.tencent.devops.process.pojo.classify.PipelineViewMatchDynamic
import com.tencent.devops.process.pojo.classify.PipelineViewPipelineCount
import com.tencent.devops.process.pojo.classify.PipelineViewPreview
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import com.tencent.devops.process.pojo.classify.PipelineViewTopForm
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineViewResourceImpl @Autowired constructor(
    private val pipelineViewService: PipelineViewService,
    private val pipelineViewGroupService: PipelineViewGroupService
) : UserPipelineViewResource {
    override fun getViewSettings(userId: String, projectId: String): Result<PipelineViewSettings> {
        return Result(pipelineViewService.getViewSettings(userId, projectId))
    }

    override fun updateViewSettings(userId: String, projectId: String, viewIdList: List<String>): Result<Boolean> {
        pipelineViewService.updateViewSettings(userId, projectId, viewIdList)
        return Result(true)
    }

    override fun getViews(userId: String, projectId: String): Result<List<PipelineNewViewSummary>> {
        return Result(pipelineViewService.getViews(userId, projectId))
    }

    override fun getView(userId: String, projectId: String, viewId: String): Result<PipelineNewView> {
        return Result(pipelineViewGroupService.getView(userId, projectId, viewId))
    }

    override fun addView(
        userId: String,
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewId> {
        return Result(PipelineViewId(pipelineViewGroupService.addViewGroup(projectId, userId, pipelineView)))
    }

    override fun listViewByPipelineId(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<PipelineNewViewSummary>> {
        return Result(pipelineViewGroupService.listViewByPipelineId(userId, projectId, pipelineId))
    }

    override fun topView(
        userId: String,
        projectId: String,
        viewId: String,
        pipelineViewTopForm: PipelineViewTopForm
    ): Result<Boolean> {
        return Result(pipelineViewService.topView(userId, projectId, viewId, pipelineViewTopForm.enabled))
    }

    override fun preview(
        userId: String,
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewPreview> {
        return Result(pipelineViewGroupService.preview(userId, projectId, pipelineView))
    }

    override fun dict(userId: String, projectId: String): Result<PipelineViewDict> {
        return Result(pipelineViewGroupService.dict(userId, projectId))
    }

    override fun getHitFilters(
        userId: String,
        projectId: String,
        pipelineId: String,
        viewId: String
    ): Result<PipelineViewHitFilters> {
        return Result(pipelineViewService.getHitFilters(userId, projectId, pipelineId, viewId))
    }

    override fun matchDynamicView(
        userId: String,
        projectId: String,
        pipelineViewMatchDynamic: PipelineViewMatchDynamic
    ): Result<List<String>> {
        return Result(pipelineViewService.matchDynamicView(userId, projectId, pipelineViewMatchDynamic))
    }

    override fun listView(
        userId: String,
        projectId: String,
        projected: Boolean?,
        viewType: Int?
    ): Result<List<PipelineNewViewSummary>> {
        return Result(pipelineViewGroupService.listView(userId, projectId, projected, viewType))
    }

    override fun pipelineCount(userId: String, projectId: String, viewId: String): Result<PipelineViewPipelineCount> {
        return Result(pipelineViewGroupService.pipelineCount(userId, projectId, viewId))
    }

    override fun listViewIdsByPipelineId(userId: String, projectId: String, pipelineId: String): Result<Set<Long>> {
        return Result(pipelineViewGroupService.listViewIdsByPipelineId(projectId, pipelineId))
    }

    override fun bulkAdd(userId: String, projectId: String, bulkAdd: PipelineViewBulkAdd): Result<Boolean> {
        return Result(pipelineViewGroupService.bulkAdd(userId, projectId, bulkAdd))
    }

    override fun bulkRemove(userId: String, projectId: String, bulkRemove: PipelineViewBulkRemove): Result<Boolean> {
        return Result(pipelineViewGroupService.bulkRemove(userId, projectId, bulkRemove))
    }

    override fun deleteView(userId: String, projectId: String, viewId: String): Result<Boolean> {
        return Result(pipelineViewGroupService.deleteViewGroup(projectId, userId, viewId))
    }

    override fun updateView(
        userId: String,
        projectId: String,
        viewId: String,
        pipelineView: PipelineViewForm
    ): Result<Boolean> {
        return Result(pipelineViewGroupService.updateViewGroup(projectId, userId, viewId, pipelineView))
    }
}
