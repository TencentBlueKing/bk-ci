/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineViewGroupCommonService @Autowired constructor(
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val dslContext: DSLContext
) {
    fun listPipelineIdsByViewIds(projectId: String, viewIdsEncode: List<String>): List<String> {
        val viewIds = viewIdsEncode.map { HashUtil.decodeIdToLong(it) }
        val pipelineIds = mutableListOf<String>()
        val viewGroups = pipelineViewGroupDao.listByViewIds(dslContext, projectId, viewIds)
        if (viewGroups.isEmpty()) {
            pipelineIds.addAll(emptyList())
        } else {
            pipelineIds.addAll(viewGroups.map { it.pipelineId }.toList())
        }
        if (pipelineIds.isEmpty()) {
            pipelineIds.add("##NONE##") // 特殊标志,避免有些判空逻辑导致过滤器没有执行
        }
        return pipelineIds
    }

    fun listViewIdsByPipelineId(projectId: String, pipelineId: String): Set<Long> {
        return pipelineViewGroupDao.listByPipelineId(dslContext, projectId, pipelineId).map { it.viewId }.toSet()
    }

    fun listViewIdsMap(projectId: String, pipelineIds: List<String>): Map<String, List<Long>> {
        return pipelineViewGroupDao.listByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).groupBy({ it.pipelineId }, { it.viewId })
    }
}
