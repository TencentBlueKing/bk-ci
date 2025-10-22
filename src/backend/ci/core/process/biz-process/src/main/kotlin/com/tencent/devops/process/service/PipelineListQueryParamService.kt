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

package com.tencent.devops.process.service

import com.tencent.devops.process.engine.pojo.PipelineFilterByLabelInfo
import com.tencent.devops.process.engine.pojo.PipelineFilterParam
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.service.label.PipelineGroupService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineListQueryParamService @Autowired constructor(
    private val pipelineGroupService: PipelineGroupService
) {

    fun generatePipelineFilterParams(
        projectId: String,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        queryDSLContext: DSLContext? = null
    ): MutableList<PipelineFilterParam> {
        val (
            filterByPipelineNames: List<PipelineViewFilterByName>,
            filterByPipelineCreators: List<PipelineViewFilterByCreator>,
            filterByPipelineLabels: List<PipelineViewFilterByLabel>
        ) = generatePipelineFilterInfo(
            projectId = projectId,
            filterByName = filterByPipelineName,
            filterByCreator = filterByCreator,
            filterByLabels = filterByLabels
        )
        val pipelineFilterParamList = mutableListOf<PipelineFilterParam>()
        val pipelineFilterParam = PipelineFilterParam(
            logic = Logic.AND,
            filterByPipelineNames = filterByPipelineNames,
            filterByPipelineCreators = filterByPipelineCreators,
            filterByLabelInfo = PipelineFilterByLabelInfo(
                filterByLabels = filterByPipelineLabels,
                labelToPipelineMap = generateLabelToPipelineMap(projectId, filterByPipelineLabels, queryDSLContext)
            )
        )
        pipelineFilterParamList.add(pipelineFilterParam)
        return pipelineFilterParamList
    }

    fun generatePipelineFilterInfo(
        projectId: String,
        filterByName: String?,
        filterByCreator: String?,
        filterByLabels: String?
    ): Triple<List<PipelineViewFilterByName>, List<PipelineViewFilterByCreator>, List<PipelineViewFilterByLabel>> {
        val filterByPipelineNames = if (filterByName.isNullOrEmpty()) {
            emptyList()
        } else {
            listOf(PipelineViewFilterByName(Condition.LIKE, filterByName))
        }

        val filterByPipelineCreators = if (filterByCreator.isNullOrEmpty()) {
            emptyList()
        } else {
            listOf(PipelineViewFilterByCreator(Condition.INCLUDE, filterByCreator.split(",")))
        }

        val filterByPipelineLabels = if (filterByLabels.isNullOrEmpty()) {
            emptyList()
        } else {
            val labelIds = filterByLabels.split(",")
            val labelGroupToLabelMap = pipelineGroupService.getGroupToLabelsMap(projectId, labelIds)

            labelGroupToLabelMap.map {
                PipelineViewFilterByLabel(Condition.INCLUDE, it.key, it.value)
            }
        }
        return Triple(filterByPipelineNames, filterByPipelineCreators, filterByPipelineLabels)
    }

    fun generateLabelToPipelineMap(
        projectId: String,
        pipelineViewFilterByLabels: List<PipelineViewFilterByLabel>,
        queryDSLContext: DSLContext? = null
    ): Map<String, List<String>>? {
        var labelToPipelineMap: Map<String, List<String>>? = null
        if (pipelineViewFilterByLabels.isNotEmpty()) {
            val labelIds = mutableListOf<String>()
            pipelineViewFilterByLabels.forEach {
                labelIds.addAll(it.labelIds)
            }
            labelToPipelineMap = pipelineGroupService.getViewLabelToPipelinesMap(
                projectId = projectId,
                labels = labelIds,
                queryDSLContext = queryDSLContext
            )
        }
        return labelToPipelineMap
    }
}
