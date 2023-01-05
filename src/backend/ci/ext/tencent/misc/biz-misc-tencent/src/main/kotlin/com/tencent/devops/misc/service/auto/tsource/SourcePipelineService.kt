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

package com.tencent.devops.misc.service.auto.tsource

import com.tencent.devops.misc.dao.auto.tsource.SourcePipelineDao
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineResourceRecord
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SourcePipelineService @Autowired constructor(
    private val dslContext: DSLContext,
    private val sourcePipelineDao: SourcePipelineDao
) {

    fun listPipelineInfos(projectId: String): Collection<TPipelineInfoRecord> {
        return sourcePipelineDao.listPipelineInfos(dslContext, projectId)
    }

    fun listPipelineBuilds(
        projectId: String,
        pipelineId: String,
        offset: Long,
        limit: Int
    ): Collection<TPipelineBuildHistoryRecord> {
        return sourcePipelineDao.listPipelineBuilds(dslContext, projectId, pipelineId, offset, limit)
    }

    fun getPipelineLatestRes(projectId: String, pipelineId: String): TPipelineResourceRecord? {
        return sourcePipelineDao.getPipelineRes(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun getPipelineSetting(projectId: String, pipelineId: String): TPipelineSettingRecord? {
        return sourcePipelineDao.getPipelineSetting(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun getPipelineSummary(projectId: String, pipelineId: String): TPipelineBuildSummaryRecord? {
        return sourcePipelineDao.getPipelineSummary(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun getPipelineBuildDetail(projectId: String, buildId: String): TPipelineBuildDetailRecord? {
        return sourcePipelineDao.getPipelineBuildDetail(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId
        )
    }
}
