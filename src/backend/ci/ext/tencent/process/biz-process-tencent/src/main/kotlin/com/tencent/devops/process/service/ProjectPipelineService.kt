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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectPipelineService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineService: PipelineService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectPipelineService::class.java)
    }

    fun listPipelinesByProjectIds(
        projectIds: Set<String>,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?
    ): Page<Pipeline> {
        val projectIdsStr = projectIds.fold("") { s1, s2 -> "$s1:$s2" }
        logger.info("listPipelinesByProjectIds|$projectIdsStr,$page,$pageSize,$channelCode")
        if (projectIds.isEmpty()) return Page(
            count = 0,
            page = PageUtil.getValidPage(page),
            pageSize = PageUtil.getValidPageSize(pageSize),
            totalPages = 1,
            records = listOf()
        )
        var totalCount = 0
        var pipelines = mutableListOf<Pipeline>()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            totalCount = pipelineService.getPipelineInfoNum(
                dslContext = context,
                projectIds = projectIds,
                channelCodes = mutableSetOf(channelCode ?: ChannelCode.BS)
            )!!
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
            pipelines = pipelineService.listPagedPipelines(
                dslContext = context,
                projectIds = projectIds,
                channelCodes = mutableSetOf(channelCode ?: ChannelCode.BS),
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
        }
        // 排序
        pipelineService.sortPipelines(pipelines, PipelineSortType.UPDATE_TIME)
        return Page(
            page = PageUtil.getValidPage(page),
            pageSize = PageUtil.getValidPageSize(pageSize),
            count = totalCount.toLong(),
            records = pipelines
        )
    }
}