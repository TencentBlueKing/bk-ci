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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.util.JooqUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_STAGE
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.InsertOnDuplicateSetMoreStep
import org.jooq.Query
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineBuildStageDao {

    fun create(
        dslContext: DSLContext,
        buildStage: PipelineBuildStage
    ) {

        val count = with(T_PIPELINE_BUILD_STAGE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                STAGE_ID,
                SEQ,
                STATUS,
                START_TIME,
                END_TIME,
                COST,
                EXECUTE_COUNT,
                CONDITIONS,
                CHECK_IN,
                CHECK_OUT
            )
                .values(
                    buildStage.projectId,
                    buildStage.pipelineId,
                    buildStage.buildId,
                    buildStage.stageId,
                    buildStage.seq,
                    buildStage.status.ordinal,
                    buildStage.startTime,
                    buildStage.endTime,
                    buildStage.cost,
                    buildStage.executeCount,
                    buildStage.controlOption?.let { self -> JsonUtil.toJson(self, formatted = false) },
                    buildStage.checkIn?.let { self -> JsonUtil.toJson(self, formatted = false) },
                    buildStage.checkOut?.let { self -> JsonUtil.toJson(self, formatted = false) }
                )
                .execute()
        }
        logger.info("save the buildStage=$buildStage, result=${count == 1}")
    }

    fun batchSave(dslContext: DSLContext, taskList: Collection<PipelineBuildStage>) {
        val records = mutableListOf<InsertOnDuplicateSetMoreStep<TPipelineBuildStageRecord>>()
        with(T_PIPELINE_BUILD_STAGE) {
            taskList.forEach {
                records.add(
                    dslContext.insertInto(this)
                        .set(PROJECT_ID, it.projectId)
                        .set(PIPELINE_ID, it.pipelineId)
                        .set(BUILD_ID, it.buildId)
                        .set(STAGE_ID, it.stageId)
                        .set(SEQ, it.seq)
                        .set(STATUS, it.status.ordinal)
                        .set(START_TIME, it.startTime)
                        .set(END_TIME, it.endTime)
                        .set(COST, it.cost)
                        .set(EXECUTE_COUNT, it.executeCount)
                        .set(CONDITIONS, it.controlOption?.let { self -> JsonUtil.toJson(self, formatted = false) })
                        .set(CHECK_IN, it.checkIn?.let { self -> JsonUtil.toJson(self, formatted = false) })
                        .set(CHECK_OUT, it.checkOut?.let { self -> JsonUtil.toJson(self, formatted = false) })
                        .onDuplicateKeyUpdate()
                        .set(STATUS, it.status.ordinal)
                        .set(START_TIME, it.startTime)
                        .set(END_TIME, it.endTime)
                        .set(COST, it.cost)
                        .set(EXECUTE_COUNT, it.executeCount)
                )
            }
        }
        dslContext.batch(records).execute()
    }

    fun batchUpdate(dslContext: DSLContext, taskList: List<TPipelineBuildStageRecord>) {
        val records = mutableListOf<Query>()
        with(T_PIPELINE_BUILD_STAGE) {
            taskList.forEach {
                records.add(
                    dslContext.update(this)
                        .set(PROJECT_ID, it.projectId)
                        .set(PIPELINE_ID, it.pipelineId)
                        .set(SEQ, it.seq)
                        .set(STATUS, it.status)
                        .set(START_TIME, it.startTime)
                        .set(END_TIME, it.endTime)
                        .set(COST, it.cost)
                        .set(EXECUTE_COUNT, it.executeCount)
                        .set(CONDITIONS, it.conditions)
                        .set(CHECK_IN, it.checkIn)
                        .set(CHECK_OUT, it.checkOut)
                        .where(BUILD_ID.eq(it.buildId).and(STAGE_ID.eq(it.stageId)))
                )
            }
        }
        dslContext.batch(records).execute()
    }

    fun get(dslContext: DSLContext, buildId: String, stageId: String?): TPipelineBuildStageRecord? {
        return with(T_PIPELINE_BUILD_STAGE) {
            val where = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId))
            if (!stageId.isNullOrBlank()) {
                where.and(STAGE_ID.eq(stageId))
            }
            where.fetchAny()
        }
    }

    fun getNextStage(dslContext: DSLContext, buildId: String, currentStageSeq: Int): TPipelineBuildStageRecord? {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId)).and(SEQ.gt(currentStageSeq))
                .orderBy(SEQ.asc())
                .limit(1)
                .fetchAny()
        }
    }

    fun listByBuildId(dslContext: DSLContext, buildId: String): Collection<TPipelineBuildStageRecord> {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this).where(BUILD_ID.eq(buildId)).orderBy(SEQ.asc()).fetch()
        }
    }

    fun deletePipelineBuildStages(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun convert(tTPipelineBuildStageRecord: TPipelineBuildStageRecord?): PipelineBuildStage? {
        if (tTPipelineBuildStageRecord != null) {
            return with(tTPipelineBuildStageRecord) {
                val controlOption = if (!conditions.isNullOrBlank()) {
                    JsonUtil.to(conditions, PipelineBuildStageControlOption::class.java)
                } else {
                    PipelineBuildStageControlOption(
                        StageControlOption(timeout = Timeout.DEFAULT_STAGE_TIMEOUT_HOURS)
                    )
                }

                val checkInOption = if (!checkIn.isNullOrBlank()) {
                    JsonUtil.to(checkIn, StagePauseCheck::class.java)
                } else {
                    // #4531 兼容旧数据运行过程时的取值
                    StagePauseCheck.convertControlOption(controlOption.stageControlOption)
                }

                val checkOutOption = if (!checkOut.isNullOrBlank()) {
                    JsonUtil.to(checkOut, StagePauseCheck::class.java)
                } else {
                    null
                }

                PipelineBuildStage(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    seq = seq,
                    status = BuildStatus.values()[status],
                    startTime = startTime,
                    endTime = endTime,
                    cost = cost ?: 0,
                    executeCount = executeCount ?: 1,
                    controlOption = controlOption,
                    checkIn = checkInOption,
                    checkOut = checkOutOption
                )
            }
        } else {
            return null
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        stageId: String,
        buildStatus: BuildStatus,
        controlOption: PipelineBuildStageControlOption? = null,
        checkIn: StagePauseCheck? = null,
        checkOut: StagePauseCheck? = null,
        initStartTime: Boolean? = false
    ): Int {
        return with(T_PIPELINE_BUILD_STAGE) {
            val update = dslContext.update(this).set(STATUS, buildStatus.ordinal)
            // 根据状态来设置字段
            if (buildStatus.isFinish() || buildStatus == BuildStatus.STAGE_SUCCESS) {
                update.set(END_TIME, LocalDateTime.now())
                update.set(
                    COST, COST + JooqUtils.timestampDiff(
                    DatePart.SECOND,
                    START_TIME.cast(java.sql.Timestamp::class.java),
                    END_TIME.cast(java.sql.Timestamp::class.java)
                )
                )
            } else if (buildStatus.isRunning() || initStartTime == true) {
                update.set(START_TIME, LocalDateTime.now())
            }
            if (controlOption != null) update.set(CONDITIONS, JsonUtil.toJson(controlOption, formatted = false))
            if (checkIn != null) update.set(CHECK_IN, JsonUtil.toJson(checkIn, formatted = false))
            if (checkOut != null) update.set(CHECK_OUT, JsonUtil.toJson(checkOut, formatted = false))
            update.where(BUILD_ID.eq(buildId)).and(STAGE_ID.eq(stageId)).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        buildId: String,
        stageId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        buildStatus: BuildStatus
    ): Int {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.update(this)
                .set(STATUS, buildStatus.ordinal)
                .set(START_TIME, startTime)
                .set(END_TIME, endTime)
                .set(
                    COST,
                    COST + JooqUtils.timestampDiff(
                        DatePart.SECOND,
                        START_TIME.cast(java.sql.Timestamp::class.java),
                        END_TIME.cast(java.sql.Timestamp::class.java)
                    )
                )
                .where(BUILD_ID.eq(buildId)).and(STAGE_ID.eq(stageId)).execute()
        }
    }

    fun getMaxStage(dslContext: DSLContext, buildId: String): TPipelineBuildStageRecord? {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId)).orderBy(SEQ.desc()).limit(1).fetchAny()
        }
    }

    fun getByStatus(dslContext: DSLContext, buildId: String, status: BuildStatus): PipelineBuildStage? {
        with(T_PIPELINE_BUILD_STAGE) {
            val data = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId)).and(STATUS.eq(status.ordinal))
                .orderBy(SEQ.asc()).limit(1).fetchAny()
            return convert(data)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildStageDao::class.java)
    }
}
