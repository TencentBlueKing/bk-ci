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

package com.tencent.devops.misc.dao.process

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.misc.pojo.project.PipelineVersionSimple
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY_DEBUG
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE_VERSION
import com.tencent.devops.model.process.tables.TPipelineBuildHisDataClear
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineBuildHistoryDebug
import com.tencent.devops.model.process.tables.TPipelineDataClear
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TPipelineOperationLog
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.pojo.PipelineOperationLog
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.Table
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

    fun getPipelineNumByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): Int {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getPipelineIdListByProjectId(
        dslContext: DSLContext,
        projectId: String,
        minId: Long,
        limit: Long,
        gapDays: Long? = null
    ): Result<out Record>? {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(ID.ge(minId))
            if (gapDays != null) {
                conditions.add(UPDATE_TIME.lt(LocalDateTime.now().minusDays(gapDays)))
            }
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

    fun getMaxPipelineBuildNum(
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
            return baseStep.orderBy(BUILD_NUM).fetch()
        }
    }

    fun getHistoryInfoList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int,
        statusList: List<BuildStatus>? = null
    ): Result<out Record>? {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            if (!statusList.isNullOrEmpty()) {
                conditions.add(STATUS.`in`(statusList.map { it.ordinal }))
            }
            return dslContext.select(BUILD_ID, CHANNEL, START_USER)
                .from(this)
                .where(conditions)
                .orderBy(BUILD_NUM).limit(limit).offset(offset).fetch()
        }
    }

    fun getHistoryDebugInfoList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int,
        statusList: List<BuildStatus>? = null
    ): Result<out Record>? {
        with(TPipelineBuildHistoryDebug.T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val conditions = mutableListOf<Condition>().apply {
                add(PROJECT_ID.eq(projectId))
                add(PIPELINE_ID.eq(pipelineId))
                if (!statusList.isNullOrEmpty()) {
                    add(STATUS.`in`(statusList.map { it.ordinal }))
                }
            }
            return dslContext.select(BUILD_ID, CHANNEL, START_USER)
                .from(this)
                .where(conditions)
                .orderBy(BUILD_NUM).limit(limit).offset(offset).fetch()
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
    ): Int? {
        with(T_PIPELINE_BUILD_HISTORY) {
            return dslContext.select(VERSION).from(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId).and(BUILD_ID.eq(buildId))))
                .fetchOne(0, Int::class.java)
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
                .set(UPDATE_TIME, DSL.field(UPDATE_TIME.name, LocalDateTime::class.java))
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
        return with(T_PIPELINE_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(VERSION.eq(version))
            dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countAllBuildWithStatus(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        status: Set<BuildStatus>
    ): Int {
        val tPipelineBuildHistory = TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY
        fun queryTableCount(table: Table<*>): Int {
            // 构造通用查询条件
            val conditions = listOf(
                table.field(tPipelineBuildHistory.PROJECT_ID.name, String::class.java)!!.eq(projectId),
                table.field(tPipelineBuildHistory.PIPELINE_ID.name, String::class.java)!!.eq(pipelineId),
                table.field(tPipelineBuildHistory.STATUS.name, Int::class.java)!!.`in`(status.map { it.ordinal }),
            )

            return dslContext.selectCount()
                .from(table)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
        return queryTableCount(T_PIPELINE_BUILD_HISTORY) + queryTableCount(T_PIPELINE_BUILD_HISTORY_DEBUG)
    }

    fun addPipelineOperationLog(
        dslContext: DSLContext,
        pipelineOperationLog: PipelineOperationLog
    ) {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                VERSION,
                OPERATOR,
                OPERATION_TYPE,
                PARAMS,
                DESCRIPTION
            ).values(
                pipelineOperationLog.id,
                pipelineOperationLog.projectId,
                pipelineOperationLog.pipelineId,
                pipelineOperationLog.version,
                pipelineOperationLog.operator,
                pipelineOperationLog.operationLogType.name,
                pipelineOperationLog.params,
                pipelineOperationLog.description
            ).execute()
        }
    }
}
