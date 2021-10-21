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

import com.tencent.devops.model.process.tables.TPipelineBuildDetail
import com.tencent.devops.model.process.tables.TPipelineBuildHisDataClear
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineDataClear
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TPipelineResource
import com.tencent.devops.model.process.tables.TPipelineResourceVersion
import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineResourceRecord
import com.tencent.devops.model.process.tables.records.TPipelineResourceVersionRecord
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
                .set(PROJECT_ID, projectId)
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
                .set(PROJECT_ID, projectId)
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
        pipelineId: String
    ): TPipelineInfoRecord? {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchAny()
        }
    }

    fun getMinPipelineInfoIdListByProjectId(
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

    fun getTotalBuildCount(
        dslContext: DSLContext,
        pipelineId: String,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        geTimeFlag: Boolean? = null
    ): Long {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            val conditions = getQueryBuildHistoryCondition(pipelineId, maxBuildNum, maxStartTime, geTimeFlag)
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun TPipelineBuildHistory.getQueryBuildHistoryCondition(
        pipelineId: String,
        maxBuildNum: Int?,
        maxStartTime: LocalDateTime?,
        geTimeFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
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

    fun getHistoryBuildIdList(
        dslContext: DSLContext,
        pipelineId: String,
        totalHandleNum: Int,
        handlePageSize: Int,
        isCompletelyDelete: Boolean,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        geTimeFlag: Boolean? = null
    ): Result<out Record>? {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            val conditions = getQueryBuildHistoryCondition(pipelineId, maxBuildNum, maxStartTime, geTimeFlag)
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

    fun getPipelineBuildDetailList(
        dslContext: DSLContext,
        buildIdList: List<String>
    ): Result<TPipelineBuildDetailRecord>? {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.`in`(buildIdList))
                .fetch()
        }
    }

    fun updatePipelineBuildDetailProject(
        dslContext: DSLContext,
        buildId: String,
        projectId: String,
        model: String? = null
    ) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            val baseStep = dslContext.update(this)
            if (!model.isNullOrBlank()) {
                baseStep.set(MODEL, model)
            }
            baseStep.set(PROJECT_ID, projectId).where(BUILD_ID.eq(buildId)).execute()
        }
    }

    fun getPipelineResourceList(
        dslContext: DSLContext,
        pipelineId: String
    ): Result<TPipelineResourceRecord>? {
        with(TPipelineResource.T_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    fun updatePipelineResourceProject(
        dslContext: DSLContext,
        pipelineId: String,
        version: Int,
        projectId: String,
        model: String? = null
    ) {
        with(TPipelineResource.T_PIPELINE_RESOURCE) {
            val baseStep = dslContext.update(this)
            if (!model.isNullOrBlank()) {
                baseStep.set(MODEL, model)
            }
            baseStep.set(PROJECT_ID, projectId)
                .where(PIPELINE_ID.eq(pipelineId).and(VERSION.eq(version)))
                .execute()
        }
    }

    fun getPipelineResourceVersionList(
        dslContext: DSLContext,
        pipelineId: String
    ): Result<TPipelineResourceVersionRecord>? {
        with(TPipelineResourceVersion.T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    fun updatePipelineResourceVersionProject(
        dslContext: DSLContext,
        pipelineId: String,
        version: Int,
        projectId: String,
        model: String? = null
    ) {
        with(TPipelineResourceVersion.T_PIPELINE_RESOURCE_VERSION) {
            val baseStep = dslContext.update(this)
            if (!model.isNullOrBlank()) {
                baseStep.set(MODEL, model)
            }
            baseStep.set(PROJECT_ID, projectId)
                .where(PIPELINE_ID.eq(pipelineId).and(VERSION.eq(version)))
                .execute()
        }
    }

    fun updateTemplatePipelineProject(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String
    ) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.update(this).set(PROJECT_ID, projectId)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}
