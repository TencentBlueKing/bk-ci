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

package com.tencent.devops.misc.dao.process

import com.tencent.devops.misc.pojo.project.PipelineVersionSimple
import com.tencent.devops.model.process.Tables
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE_VERSION
import com.tencent.devops.model.process.tables.TPipelineBuildHisDataClear
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineDataClear
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("LongParameterList", "TooManyFunctions")
@Repository
class ProcessDao {

    fun addBuildHisDataClear(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        with(TPipelineBuildHisDataClear.T_PIPELINE_BUILD_HIS_DATA_CLEAR) {
            dslContext.insertInto(this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID)
                .values(
                    projectId,
                    pipelineId,
                    buildId
                ).onDuplicateKeyUpdate()
                .set(BUILD_ID, buildId)
                .execute()
        }
    }

    fun addPipelineDataClear(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) {
        with(TPipelineDataClear.T_PIPELINE_DATA_CLEAR) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID
            )
                .values(
                    projectId,
                    pipelineId
                ).onDuplicateKeyUpdate()
                .set(PIPELINE_ID, pipelineId)
                .execute()
        }
    }

    fun getPipelineIdListByProjectId(
        dslContext: DSLContext,
        projectId: String,
        minId: Long,
        limit: Long
    ): Result<out Record>? {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(ID.ge(minId))
            return dslContext.select(PIPELINE_ID).from(this)
                .where(conditions)
                .orderBy(ID.asc())
                .limit(limit)
                .fetch()
        }
    }

    fun getPipelineInfoByPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineInfoRecord? {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchAny()
        }
    }

    fun getMinPipelineInfoIdByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): Long {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            return dslContext.select(DSL.min(ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getMaxPipelineBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Long {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            return dslContext.select(DSL.max(BUILD_NUM))
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getMinPipelineBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Long {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            return dslContext.select(DSL.min(BUILD_NUM))
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getTotalBuildCount(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        geTimeFlag: Boolean? = null
    ): Long {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            val conditions = getQueryBuildHistoryCondition(
                projectId = projectId,
                pipelineId = pipelineId,
                maxBuildNum = maxBuildNum,
                maxStartTime = maxStartTime,
                geTimeFlag = geTimeFlag
            )
            return dslContext.select(DSL.max(BUILD_NUM))
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun TPipelineBuildHistory.getQueryBuildHistoryCondition(
        projectId: String,
        pipelineId: String,
        maxBuildNum: Int?,
        maxStartTime: LocalDateTime?,
        geTimeFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(PROJECT_ID.eq(projectId))
        conditions.add(PIPELINE_ID.eq(pipelineId))
        if (maxBuildNum != null) {
            conditions.add(BUILD_NUM.le(maxBuildNum))
        }
        if (maxStartTime != null) {
            if (geTimeFlag != true) {
                conditions.add(START_TIME.lt(maxStartTime))
            } else {
                conditions.add(START_TIME.ge(maxStartTime))
            }
        }
        return conditions
    }

    @Suppress("LongParameterList")
    fun getHistoryBuildIdList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        totalHandleNum: Int,
        handlePageSize: Int,
        isCompletelyDelete: Boolean,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        geTimeFlag: Boolean? = null
    ): Result<out Record>? {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            val conditions = getQueryBuildHistoryCondition(
                projectId = projectId,
                pipelineId = pipelineId,
                maxBuildNum = maxBuildNum,
                maxStartTime = maxStartTime,
                geTimeFlag = geTimeFlag
            )
            val baseStep = dslContext.select(BUILD_ID)
                .from(this)
                .where(conditions)
            if (isCompletelyDelete) {
                baseStep.limit(handlePageSize)
            } else {
                baseStep.limit(totalHandleNum, handlePageSize)
            }
            return baseStep.orderBy(BUILD_ID).fetch()
        }
    }

    fun getClearDeletePipelineIdList(
        dslContext: DSLContext,
        projectId: String,
        pipelineIdList: List<String>,
        gapDays: Long
    ): Result<out Record>? {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            return dslContext.select(PIPELINE_ID).from(this)
                .where(
                    PROJECT_ID.eq(projectId)
                        .and(DELETE.eq(true))
                        .and(UPDATE_TIME.lt(LocalDateTime.now().minusDays(gapDays)))
                        .and(PIPELINE_ID.`in`(pipelineIdList))
                )
                .fetch()
        }
    }

    fun getPipelineVersionByBuildId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Int {
        with(Tables.T_PIPELINE_BUILD_HISTORY) {
            return dslContext.select(VERSION).from(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId).and(BUILD_ID.eq(buildId))))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getPipelineVersionSimple(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineVersionSimple? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.select(
                PIPELINE_ID,
                CREATOR,
                CREATE_TIME,
                VERSION,
                VERSION_NAME,
                REFER_FLAG,
                REFER_COUNT
            )
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version)))
                .fetchOneInto(PipelineVersionSimple::class.java)
        }
    }

    fun updatePipelineVersionReferInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        referCount: Int,
        referFlag: Boolean? = null
    ) {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val baseStep = dslContext.update(this)
                .set(REFER_COUNT, referCount)
            referFlag?.let { baseStep.set(REFER_FLAG, referFlag) }
            baseStep.where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version))).execute()
        }
    }

    fun countBuildNumByVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Int {
        return with(Tables.T_PIPELINE_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(VERSION.eq(version))
            dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }
}
