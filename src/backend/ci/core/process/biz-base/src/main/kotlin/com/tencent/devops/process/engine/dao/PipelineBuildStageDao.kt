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
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_STAGE
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.RecordMapper
import org.jooq.util.mysql.MySQLDSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineBuildStageDao {

    fun create(dslContext: DSLContext, buildStage: PipelineBuildStage) {

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

    fun batchSave(dslContext: DSLContext, stageList: Collection<PipelineBuildStage>) {
        with(T_PIPELINE_BUILD_STAGE) {
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
            ).also { insert ->
                stageList.forEach {
                    insert.values(
                        it.projectId,
                        it.pipelineId,
                        it.buildId,
                        it.stageId,
                        it.seq,
                        it.status.ordinal,
                        it.startTime,
                        it.endTime,
                        it.cost,
                        it.executeCount,
                        it.controlOption?.let { self -> JsonUtil.toJson(self, formatted = false) },
                        it.checkIn?.let { self -> JsonUtil.toJson(self, formatted = false) },
                        it.checkOut?.let { self -> JsonUtil.toJson(self, formatted = false) }
                    )
                }
            }.onDuplicateKeyUpdate()
                .set(STATUS, MySQLDSL.values(STATUS))
                .set(START_TIME, MySQLDSL.values(START_TIME))
                .set(END_TIME, MySQLDSL.values(END_TIME))
                .set(COST, MySQLDSL.values(COST))
                .set(EXECUTE_COUNT, MySQLDSL.values(EXECUTE_COUNT))
                .execute()
        }
    }

    fun batchUpdate(dslContext: DSLContext, stageList: Collection<PipelineBuildStage>) {
        with(T_PIPELINE_BUILD_STAGE) {
            stageList.forEach {
                dslContext.update(this)
                    .set(PIPELINE_ID, it.pipelineId)
                    .set(SEQ, it.seq)
                    .set(STATUS, it.status.ordinal)
                    .set(START_TIME, it.startTime)
                    .set(END_TIME, it.endTime)
                    .set(COST, it.cost)
                    .set(EXECUTE_COUNT, it.executeCount)
                    .set(CONDITIONS, it.controlOption?.let { self -> JsonUtil.toJson(self, formatted = false) })
                    .set(CHECK_IN, it.checkIn?.let { self -> JsonUtil.toJson(self, formatted = false) })
                    .set(CHECK_OUT, it.checkOut?.let { self -> JsonUtil.toJson(self, formatted = false) })
                    .where(BUILD_ID.eq(it.buildId).and(STAGE_ID.eq(it.stageId)).and(PROJECT_ID.eq(it.projectId)))
                    .execute()
            }
        }
    }

    fun get(dslContext: DSLContext, projectId: String, buildId: String, stageId: String?): PipelineBuildStage? {
        return with(T_PIPELINE_BUILD_STAGE) {
            val where = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            if (!stageId.isNullOrBlank()) {
                where.and(STAGE_ID.eq(stageId))
            }
            where.fetchAny(mapper)
        }
    }

    fun getByBuildId(dslContext: DSLContext, projectId: String, buildId: String): Collection<PipelineBuildStage> {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
                .fetch(mapper)
        }
    }

    /**
     * 取相邻的，大于或小于[currentStageSeq]序号的Stage：
     * 当[sortAsc] = true 时，取[currentStageSeq]之后相邻的第一个Stage
     * 当[sortAsc] = false 时，取[currentStageSeq]之前相邻的第一个Stage
     */
    fun getAdjacentStage(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        currentStageSeq: Int,
        sortAsc: Boolean
    ): PipelineBuildStage? {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId)).and(PROJECT_ID.eq(projectId))
                .let { condition ->
                    if (sortAsc) {
                        condition.and(SEQ.gt(currentStageSeq)).orderBy(SEQ.asc())
                    } else {
                        condition.and(SEQ.lt(currentStageSeq)).orderBy(SEQ.desc())
                    }
                }
                .limit(1)
                .fetchAny(mapper)
        }
    }

    fun listBuildStages(dslContext: DSLContext, projectId: String, buildId: String): List<PipelineBuildStage> {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
                .orderBy(SEQ.asc()).fetch(mapper)
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

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        stageId: String,
        buildStatus: BuildStatus?,
        controlOption: PipelineBuildStageControlOption? = null,
        checkIn: StagePauseCheck? = null,
        checkOut: StagePauseCheck? = null,
        initStartTime: Boolean? = false
    ): Int {
        return with(T_PIPELINE_BUILD_STAGE) {
            val update = dslContext.update(this).set(STAGE_ID, stageId)
            // 根据状态来设置字段
            if (buildStatus?.isFinish() == true || buildStatus == BuildStatus.STAGE_SUCCESS) {
                update.set(END_TIME, LocalDateTime.now())
                update.set(
                    COST,
                    COST + JooqUtils.timestampDiff(
                        DatePart.SECOND, START_TIME.cast(java.sql.Timestamp::class.java),
                        END_TIME.cast(java.sql.Timestamp::class.java)
                    )
                )
            } else if (buildStatus?.isRunning() == true || initStartTime == true) {
                update.set(START_TIME, LocalDateTime.now())
            }
            buildStatus?.let { update.set(STATUS, it.ordinal) }
            controlOption?.let { update.set(CONDITIONS, JsonUtil.toJson(it, formatted = false)) }
            checkIn?.let { update.set(CHECK_IN, JsonUtil.toJson(it, formatted = false)) }
            checkOut?.let { update.set(CHECK_OUT, JsonUtil.toJson(it, formatted = false)) }
            update.where(BUILD_ID.eq(buildId)).and(STAGE_ID.eq(stageId)).and(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun getMaxStage(dslContext: DSLContext, projectId: String, buildId: String): PipelineBuildStage? {
        return with(T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
                .orderBy(SEQ.desc()).limit(1).fetchOne(mapper)
        }
    }

    fun getOneByStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        statusSet: Set<BuildStatus>
    ): PipelineBuildStage? {
        with(T_PIPELINE_BUILD_STAGE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId)).and(BUILD_ID.eq(buildId))
                .and(STATUS.`in`(statusSet.map { it.ordinal }))
                .orderBy(SEQ.asc()).limit(1).fetchOne(mapper)
        }
    }

    class PipelineBuildStageJooqMapper : RecordMapper<TPipelineBuildStageRecord, PipelineBuildStage> {
        override fun map(record: TPipelineBuildStageRecord?): PipelineBuildStage? {
            return record?.run {
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
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildStageDao::class.java)
        private val mapper = PipelineBuildStageJooqMapper()
    }
}
