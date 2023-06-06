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

package com.tencent.devops.process.dao.record

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.model.process.tables.TPipelineBuildRecordStage
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordStageRecord
import com.tencent.devops.process.pojo.KEY_EXECUTE_COUNT
import com.tencent.devops.process.pojo.KEY_STAGE_ID
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.jooq.util.mysql.MySQLDSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList")
class BuildRecordStageDao {

    fun batchSave(dslContext: DSLContext, records: List<BuildRecordStage>) {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            dslContext.insertInto(
                this,
                BUILD_ID,
                PROJECT_ID,
                PIPELINE_ID,
                RESOURCE_VERSION,
                STAGE_ID,
                EXECUTE_COUNT,
                SEQ,
                STAGE_VAR,
                STATUS,
                TIMESTAMPS
            ).also { insertSetStep ->
                records.forEach { record ->
                    insertSetStep.values(
                        record.buildId,
                        record.projectId,
                        record.pipelineId,
                        record.resourceVersion,
                        record.stageId,
                        record.executeCount,
                        record.stageSeq,
                        JsonUtil.toJson(record.stageVar, false),
                        record.status,
                        JsonUtil.toJson(record.timestamps, false)
                    )
                }
            }.onDuplicateKeyUpdate()
                .set(STATUS, MySQLDSL.values(STATUS))
                .set(START_TIME, MySQLDSL.values(START_TIME))
                .set(END_TIME, MySQLDSL.values(END_TIME))
                .set(TIMESTAMPS, MySQLDSL.values(TIMESTAMPS))
                .set(STAGE_VAR, MySQLDSL.values(STAGE_VAR))
                .execute()
        }
    }

    fun updateRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        stageVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            val update = dslContext.update(this)
                .set(STAGE_VAR, JsonUtil.toJson(stageVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
            startTime?.let { update.set(START_TIME, startTime) }
            endTime?.let { update.set(END_TIME, endTime) }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(STAGE_ID.eq(stageId))
            ).execute()
        }
    }

    fun getRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatusSet: Set<BuildStatus>? = null
    ): List<BuildRecordStage> {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(EXECUTE_COUNT.eq(executeCount))
            buildStatusSet?.let { conditions.add(STATUS.`in`(it.map { status -> status.name })) }
            return dslContext.selectFrom(this)
                .where(conditions).orderBy(STAGE_ID.asc()).fetch(mapper)
        }
    }

    fun getLatestRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): List<BuildRecordStage> {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            val conditions = BUILD_ID.eq(buildId)
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(EXECUTE_COUNT.lessOrEqual(executeCount))
            val max = DSL.select(
                STAGE_ID.`as`(KEY_STAGE_ID),
                DSL.max(EXECUTE_COUNT).`as`(KEY_EXECUTE_COUNT)
            ).from(this).where(conditions).groupBy(STAGE_ID)
            val result = dslContext.select(
                BUILD_ID, PROJECT_ID, PIPELINE_ID, RESOURCE_VERSION, STAGE_ID, SEQ,
                EXECUTE_COUNT, STATUS, STAGE_VAR, START_TIME, END_TIME, TIMESTAMPS
            ).from(this).join(max).on(
                STAGE_ID.eq(max.field(KEY_STAGE_ID, String::class.java))
                    .and(EXECUTE_COUNT.eq(max.field(KEY_EXECUTE_COUNT, Int::class.java)))
            ).where(conditions).orderBy(SEQ.asc())
                .fetch()
            return result.map { record ->
                BuildRecordStage(
                    buildId = record[BUILD_ID],
                    projectId = record[PROJECT_ID],
                    pipelineId = record[PIPELINE_ID],
                    resourceVersion = record[RESOURCE_VERSION],
                    stageId = record[STAGE_ID],
                    stageSeq = record[SEQ],
                    executeCount = record[EXECUTE_COUNT],
                    status = record[STATUS],
                    stageVar = JsonUtil.to(
                        record[STAGE_VAR], object : TypeReference<MutableMap<String, Any>>() {}
                    ),
                    startTime = record[START_TIME],
                    endTime = record[END_TIME],
                    timestamps = record[TIMESTAMPS]?.let {
                        JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
                    } ?: mapOf()
                )
            }
        }
    }

    class BuildRecordStageJooqMapper : RecordMapper<TPipelineBuildRecordStageRecord, BuildRecordStage> {
        override fun map(record: TPipelineBuildRecordStageRecord?): BuildRecordStage? {
            return record?.run {
                BuildRecordStage(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    resourceVersion = resourceVersion,
                    executeCount = executeCount,
                    stageId = stageId,
                    stageVar = JsonUtil.to(stageVar, object : TypeReference<Map<String, Any>>() {}).toMutableMap(),
                    stageSeq = seq,
                    status = status,
                    startTime = startTime,
                    endTime = endTime,
                    timestamps = timestamps?.let {
                        JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
                    } ?: mapOf()
                )
            }
        }
    }

    companion object {
        private val mapper = BuildRecordStageJooqMapper()
    }
}
