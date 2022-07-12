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

package com.tencent.devops.process.service.view

import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.dao.label.PipelineViewTopDao
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.service.label.PipelineGroupService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineViewGroupService @Autowired constructor(
    private val pipelineViewService: PipelineViewService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val pipelineViewTopDao: PipelineViewTopDao,
    private val dslContext: DSLContext
) {
    fun updateGroupAfterPipelineCreate(pipelineInfo: TPipelineInfoRecord) {
        // TODO 加锁

    }

    fun updateGroupAfterPipelineDelete(pipelineId: String) {
        // TODO 加锁
        pipelineViewGroupDao.listByPipelineId(dslContext, pipelineId).forEach {
            pipelineViewGroupDao.removeById(dslContext, it.id)
        }
    }

    fun updateGroupAfterPipelineUpdate(pipelineInfo: TPipelineInfoRecord, userId: String) {
        // TODO 加锁
        // 所有的动态项目组
        val dynamicProjectViews = pipelineViewDao.list(dslContext, pipelineInfo.projectId)
            .filter { it.viewType == PipelineViewType.DYNAMIC }
        val dynamicProjectViewIds = dynamicProjectViews.asSequence()
            .map { it.id }
            .toSet()
        // 命中的动态项目组
        val matchViewIds = dynamicProjectViews.asSequence()
            .filter { matchView(it, pipelineInfo) }
            .map { it.id }
            .toSet()
        // 已有的动态项目组
        val baseViewGroups = pipelineViewGroupDao.listByPipelineId(dslContext, pipelineInfo.pipelineId)
            .filter { dynamicProjectViewIds.contains(it.viewId) }
            .toSet()
        val baseViewIds = baseViewGroups.map { it.viewId }.toSet()
        // 新增新命中的项目组
        val addViewIds = matchViewIds.filterNot { baseViewIds.contains(it) }.toSet()
        addViewIds.forEach {
            pipelineViewGroupDao.create(
                dslContext = dslContext,
                viewId = it,
                pipelineId = pipelineInfo.pipelineId,
                creator = userId
            )
        }
        // 删除未命中的老项目组
        val deleteIds = baseViewGroups.filterNot { matchViewIds.contains(it.viewId) }.map { it.id }.toSet()
        deleteIds.forEach {
            pipelineViewGroupDao.removeById(dslContext, it)
        }
    }

    @SuppressWarnings("LoopWithTooManyJumpStatements")
    private fun matchView(
        pipelineView: TPipelineViewRecord,
        pipelineInfo: TPipelineInfoRecord
    ): Boolean {
        val filters = pipelineViewService.getFilters(
            filterByName = pipelineView.filterByPipeineName,
            filterByCreator = pipelineView.filterByCreator,
            filters = pipelineView.filters
        )
        for (filter in filters) {
            val match = if (filter is PipelineViewFilterByName) {
                pipelineInfo.pipelineName.contains(filter.pipelineName)
            } else if (filter is PipelineViewFilterByCreator) {
                filter.userIds.contains(pipelineInfo.creator)
            } else if (filter is PipelineViewFilterByLabel) {
                pipelineGroupService.getViewLabelToPipelinesMap(
                    pipelineInfo.projectId,
                    filter.labelIds
                ).values.asSequence().flatten().contains(pipelineInfo.pipelineId)
            } else {
                continue
            }

            if (pipelineView.logic == Logic.OR.name && match) {
                return true
            }
            if (pipelineView.logic == Logic.AND.name && !match) {
                return false
            }
        }
        return pipelineView.logic == Logic.AND.name
    }
}
