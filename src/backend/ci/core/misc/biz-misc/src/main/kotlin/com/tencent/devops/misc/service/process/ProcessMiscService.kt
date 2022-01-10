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

package com.tencent.devops.misc.service.process

import com.tencent.devops.misc.dao.process.ProcessDao
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("LongParameterList", "TooManyFunctions")
@Service
class ProcessMiscService @Autowired constructor(
    private val dslContext: DSLContext,
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
        geTimeFlag: Boolean? = null
    ): List<String>? {
        val historyBuildIdRecords = processDao.getHistoryBuildIdList(
            dslContext = dslContext,
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
        limit: Long
    ): List<String>? {
        val pipelineIdRecords = processDao.getPipelineIdListByProjectId(
            dslContext = dslContext,
            projectId = projectId,
            minId = minId,
            limit = limit
        )
        return generateIdList(pipelineIdRecords)
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

    fun getMinPipelineInfoIdListByProjectId(projectId: String): Long {
        return processDao.getMinPipelineInfoIdListByProjectId(dslContext, projectId)
    }

    fun getPipelineInfoIdListByPipelineId(projectId: String, pipelineId: String): Long {
        return processDao.getPipelineInfoByPipelineId(dslContext, projectId, pipelineId)?.id ?: 0L
    }

    fun getMaxPipelineBuildNum(
        projectId: String,
        pipelineId: String
    ): Long {
        return processDao.getMaxPipelineBuildNum(dslContext, projectId, pipelineId)
    }

    fun getTotalBuildCount(
        projectId: String,
        pipelineId: String,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        geTimeFlag: Boolean? = null
    ): Long {
        return processDao.getTotalBuildCount(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            maxBuildNum = maxBuildNum,
            maxStartTime = maxStartTime,
            geTimeFlag = geTimeFlag
        )
    }
}
