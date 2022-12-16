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
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

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

    fun getRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        containerId: String? = null
    ): List<BuildRecordTask> {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(EXECUTE_COUNT.eq(executeCount))
            containerId?.let { conditions.add(CONTAINER_ID.eq(containerId)) }
            return dslContext.selectFrom(this)
                .where(conditions).orderBy(TASK_SEQ.asc()).fetch(mapper)
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

    fun getRecordTaskVar(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): Map<String, Any>? {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            return dslContext.select(TASK_VAR)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(TASK_ID.eq(taskId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchOne(0, String::class.java)?.let {
                    JsonUtil.getObjectMapper().readValue(it) as Map<String, Any>
                }
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
