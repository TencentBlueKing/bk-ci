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
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.model.process.tables.TPipelineBuildRecordTask
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordTaskRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.process.pojo.KEY_EXECUTE_COUNT
import com.tencent.devops.process.pojo.KEY_TASK_ID
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import kotlin.math.max

@Repository
class BuildRecordTaskDao {

    fun batchSave(dslContext: DSLContext, records: List<BuildRecordTask>) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            records.forEach { record ->
                dslContext.insertInto(this)
                    .set(BUILD_ID, record.buildId)
                    .set(PROJECT_ID, record.projectId)
                    .set(PIPELINE_ID, record.pipelineId)
                    .set(RESOURCE_VERSION, record.resourceVersion)
                    .set(STAGE_ID, record.stageId)
                    .set(CONTAINER_ID, record.containerId)
                    .set(TASK_ID, record.taskId)
                    .set(EXECUTE_COUNT, record.executeCount)
                    .set(CLASS_TYPE, record.classType)
                    .set(ORIGIN_CLASS_TYPE, record.originClassType)
                    .set(TASK_VAR, JsonUtil.toJson(record.taskVar, false))
                    .set(STATUS, record.status)
                    .set(TASK_SEQ, record.taskSeq)
                    .set(ATOM_CODE, record.atomCode)
                    .set(TIMESTAMPS, JsonUtil.toJson(record.timestamps, false))
                    .execute()
            }
        }
    }

    fun updateRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        taskVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val update = dslContext.update(this)
                .set(TASK_VAR, JsonUtil.toJson(taskVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(TASK_ID.eq(taskId))
            ).execute()
        }
    }

    fun getLatestRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): List<BuildRecordTask> {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val conditions = BUILD_ID.eq(buildId)
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            // 获取每个最大执行次数
            val max = dslContext.select(
                TASK_ID.`as`(KEY_TASK_ID),
                DSL.max(EXECUTE_COUNT).`as`(KEY_EXECUTE_COUNT)
            ).from(this).where(conditions).groupBy(TASK_ID)
            val result = dslContext.select(
                BUILD_ID, PROJECT_ID, PIPELINE_ID, RESOURCE_VERSION, STAGE_ID, CONTAINER_ID, TASK_ID,
                TASK_SEQ, EXECUTE_COUNT, TASK_VAR, CLASS_TYPE, ATOM_CODE, STATUS, ORIGIN_CLASS_TYPE, TIMESTAMPS
            ).from(this).join(max).on(
                TASK_ID.eq(max.field(KEY_TASK_ID, String::class.java))
                    .and(EXECUTE_COUNT.eq(max.field(KEY_EXECUTE_COUNT, Int::class.java)))
            ).where(conditions).orderBy(TASK_SEQ.asc())
                .fetch()
            return result.map { record ->
                BuildRecordTask(
                    buildId = record[BUILD_ID],
                    projectId = record[PROJECT_ID],
                    pipelineId = record[PIPELINE_ID],
                    resourceVersion = record[RESOURCE_VERSION],
                    stageId = record[STAGE_ID],
                    containerId = record[CONTAINER_ID],
                    taskId = record[TASK_ID],
                    taskSeq = record[TASK_SEQ],
                    executeCount = record[EXECUTE_COUNT],
                    taskVar = JsonUtil.to(record[TASK_VAR], object : TypeReference<Map<String, Any>>() {}).toMutableMap(),
                    classType = record[CLASS_TYPE],
                    atomCode = record[ATOM_CODE],
                    status = record[STATUS],
                    originClassType = record[ORIGIN_CLASS_TYPE],
                    timestamps = record[TIMESTAMPS]?.let {
                        JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
                    } ?: mapOf()
                )
            }
        }
    }

    fun getRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): BuildRecordTask? {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            return dslContext.selectFrom(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(TASK_ID.eq(taskId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchOne(mapper)
        }
    }

    class BuildRecordTaskJooqMapper : RecordMapper<TPipelineBuildRecordTaskRecord, BuildRecordTask> {
        override fun map(record: TPipelineBuildRecordTaskRecord?): BuildRecordTask? {
            return record?.run {
                BuildRecordTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    resourceVersion = resourceVersion,
                    executeCount = executeCount,
                    stageId = stageId,
                    containerId = containerId,
                    taskId = taskId,
                    taskVar = JsonUtil.to(taskVar, object : TypeReference<Map<String, Any>>() {}).toMutableMap(),
                    taskSeq = taskSeq,
                    classType = classType,
                    atomCode = atomCode,
                    originClassType = originClassType,
                    status = status,
                    timestamps = timestamps?.let {
                        JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
                    } ?: mapOf()
                )
            }
        }
    }

    companion object {
        private val mapper = BuildRecordTaskJooqMapper()
    }
}
