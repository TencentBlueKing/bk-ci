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

package com.tencent.devops.misc.service.process

import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.misc.dao.process.ProcessDao
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("LongParameterList", "TooManyFunctions")
@Service
class ProcessMiscService @Autowired constructor(
    private val dslContext: DSLContext,
    @Qualifier(ARCHIVE_SHARDING_DSL_CONTEXT)
    private val archiveShardingDslContext: DSLContext,
    private val processDao: ProcessDao
) {

    fun getHistoryBuildIdList(
        projectId: String,
        pipelineId: String,
        totalHandleNum: Int,
        handlePageSize: Int,
        isCompletelyDelete: Boolean,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        geTimeFlag: Boolean? = null,
        archiveFlag: Boolean? = null
    ): List<String>? {
        val historyBuildIdRecords = processDao.getHistoryBuildIdList(
            dslContext = generateQueryDslContext(archiveFlag),
            projectId = projectId,
            pipelineId = pipelineId,
            totalHandleNum = totalHandleNum,
            handlePageSize = handlePageSize,
            isCompletelyDelete = isCompletelyDelete,
            maxBuildNum = maxBuildNum,
            maxStartTime = maxStartTime,
            geTimeFlag = geTimeFlag
        )
        return generateIdList(historyBuildIdRecords)
    }

    fun getClearDeletePipelineIdList(
        projectId: String,
        pipelineIdList: List<String>,
        gapDays: Long
    ): List<String>? {
        val pipelineIdRecords = processDao.getClearDeletePipelineIdList(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIdList = pipelineIdList,
            gapDays = gapDays
        )
        return generateIdList(pipelineIdRecords)
    }

    fun getPipelineIdListByProjectId(
        projectId: String,
        minId: Long,
        limit: Long,
        archiveFlag: Boolean? = null,
        gapDays: Long? = null
    ): List<String>? {
        val pipelineIdRecords = processDao.getPipelineIdListByProjectId(
            dslContext = generateQueryDslContext(archiveFlag),
            projectId = projectId,
            minId = minId,
            limit = limit,
            gapDays = gapDays
        )
        return generateIdList(pipelineIdRecords)
    }

    private fun generateQueryDslContext(archiveFlag: Boolean?): DSLContext {
        val queryDslContext = if (archiveFlag == true) {
            archiveShardingDslContext
        } else {
            dslContext
        }
        return queryDslContext
    }

    private fun generateIdList(records: Result<out Record>?): MutableList<String>? {
        return if (records == null) {
            null
        } else {
            val idList = mutableListOf<String>()
            records.forEach { record ->
                idList.add(record.getValue(0) as String)
            }
            idList
        }
    }

    fun getMinPipelineInfoIdByProjectId(projectId: String, archiveFlag: Boolean? = null): Long {
        return processDao.getMinPipelineInfoIdByProjectId(generateQueryDslContext(archiveFlag), projectId)
    }

    fun getPipelineInfoIdByPipelineId(projectId: String, pipelineId: String, archiveFlag: Boolean? = null): Long {
        return processDao.getPipelineInfoByPipelineId(
            dslContext = generateQueryDslContext(archiveFlag),
            projectId = projectId,
            pipelineId = pipelineId
        )?.id ?: 0L
    }

    fun getMaxPipelineBuildNum(
        projectId: String,
        pipelineId: String
    ): Long {
        return processDao.getMaxPipelineBuildNum(dslContext, projectId, pipelineId)
    }

    fun getMinPipelineBuildNum(
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean? = null
    ): Long {
        return processDao.getMinPipelineBuildNum(generateQueryDslContext(archiveFlag), projectId, pipelineId)
    }

    fun getMaxPipelineBuildNum(
        projectId: String,
        pipelineId: String,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        geTimeFlag: Boolean? = null,
        archiveFlag: Boolean? = null
    ): Long {
        return processDao.getMaxPipelineBuildNum(
            dslContext = generateQueryDslContext(archiveFlag),
            projectId = projectId,
            pipelineId = pipelineId,
            maxBuildNum = maxBuildNum,
            maxStartTime = maxStartTime,
            geTimeFlag = geTimeFlag
        )
    }
}
