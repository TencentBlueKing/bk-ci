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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_STAGE
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildStageDao {

    fun create(
        dslContext: DSLContext,
        buildStage: PipelineBuildStage
    ) {

        val count =
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
                    EXECUTE_COUNT
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
                        buildStage.executeCount
                    )
                    .execute()
            }
        logger.info("save the buildStage=$buildStage, result=${count == 1}")
    }

    fun batchSave(dslContext: DSLContext, taskList: Collection<PipelineBuildStage>) {
        val records = mutableListOf<TPipelineBuildStageRecord>()
        taskList.forEach {
            with(it) {
                records.add(
                    TPipelineBuildStageRecord(
                        projectId, pipelineId, buildId, stageId, seq,
                        status.ordinal, startTime, endTime, cost, executeCount
                    )
                )
            }
        }
        dslContext.batchStore(records).execute()
    }

    fun get(
        dslContext: DSLContext,
        buildId: String,
        stageId: String?
    ): TPipelineBuildStageRecord? {

        return with(T_PIPELINE_BUILD_STAGE) {

            val where = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId))
            if (!stageId.isNullOrBlank()) {
                where.and(STAGE_ID.eq(stageId))
            }
            where.fetchAny()
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

    fun convert(tTPipelineBuildStageRecord: TPipelineBuildStageRecord): PipelineBuildStage? {
        return with(tTPipelineBuildStageRecord) {
            PipelineBuildStage(
                projectId, pipelineId, buildId, stageId, seq, BuildStatus.values()[status],
                startTime, endTime, cost ?: 0, executeCount ?: 1
            )
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        stageId: String,
        buildStatus: BuildStatus
    ): Int {
        return with(T_PIPELINE_BUILD_STAGE) {
            val update = dslContext.update(this).set(STATUS, buildStatus.ordinal)
            // 根据状态来设置字段
            if (BuildStatus.isFinish(buildStatus)) {
                update.set(END_TIME, LocalDateTime.now())
                update.set(COST, COST + END_TIME - START_TIME)
            } else if (BuildStatus.isRunning(buildStatus)) {
                update.set(START_TIME, LocalDateTime.now())
            }

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
                .set(COST, COST + END_TIME - START_TIME)
                .where(BUILD_ID.eq(buildId)).and(STAGE_ID.eq(stageId)).execute()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildStageDao::class.java)
    }
}
