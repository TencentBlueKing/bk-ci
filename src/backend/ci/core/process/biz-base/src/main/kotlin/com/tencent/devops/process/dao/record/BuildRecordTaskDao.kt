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
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.model.process.tables.TPipelineBuildRecordTask
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordTaskRecord
import com.tencent.devops.process.pojo.KEY_EXECUTE_COUNT
import com.tencent.devops.process.pojo.KEY_TASK_ID
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record18
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.jooq.util.mysql.MySQLDSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList")
class BuildRecordTaskDao {

    fun batchSave(dslContext: DSLContext, records: List<BuildRecordTask>) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {

            dslContext.insertInto(
                this,
                BUILD_ID,
                PROJECT_ID,
                PIPELINE_ID,
                RESOURCE_VERSION,
                STAGE_ID,
                CONTAINER_ID,
                TASK_ID,
                EXECUTE_COUNT,
                CLASS_TYPE,
                ORIGIN_CLASS_TYPE,
                TASK_VAR,
                STATUS,
                TASK_SEQ,
                ATOM_CODE,
                TIMESTAMPS,
                POST_INFO
            ).also { insert ->
                records.forEach { record ->
                    insert.values(
                        record.buildId,
                        record.projectId,
                        record.pipelineId,
                        record.resourceVersion,
                        record.stageId,
                        record.containerId,
                        record.taskId,
                        record.executeCount,
                        record.classType,
                        record.originClassType,
                        JsonUtil.toJson(record.taskVar, false),
                        record.status,
                        record.taskSeq,
                        record.atomCode,
                        JsonUtil.toJson(record.timestamps, false),
                        record.elementPostInfo?.let { JsonUtil.toJson(it, false) }
                    )
                }
            }.onDuplicateKeyUpdate()
                .set(STATUS, MySQLDSL.values(STATUS))
                .set(START_TIME, MySQLDSL.values(START_TIME))
                .set(END_TIME, MySQLDSL.values(END_TIME))
                .set(TIMESTAMPS, MySQLDSL.values(TIMESTAMPS))
                .set(TASK_VAR, MySQLDSL.values(TASK_VAR))
                .execute()
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
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val update = dslContext.update(this)
                .set(TASK_VAR, JsonUtil.toJson(taskVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
            startTime?.let { update.set(START_TIME, startTime) }
            endTime?.let { update.set(END_TIME, endTime) }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(TASK_ID.eq(taskId))
            ).execute()
        }
    }

    fun updateRecordStatus(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        stageId: String? = null,
        containerId: String? = null
    ) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val update = dslContext.update(this)
                .set(STATUS, buildStatus.name)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                )
            stageId?.let { update.and(STAGE_ID.eq(stageId)) }
            containerId?.let { update.and(CONTAINER_ID.eq(containerId)) }
            update.execute()
        }
    }

    fun getRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        containerId: String? = null,
        buildStatusSet: Set<BuildStatus>? = null
    ): List<BuildRecordTask> {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(EXECUTE_COUNT.eq(executeCount))
            containerId?.let { conditions.add(CONTAINER_ID.eq(containerId)) }
            buildStatusSet?.let { conditions.add(STATUS.`in`(it.map { status -> status.name })) }
            return dslContext.selectFrom(this)
                .where(conditions).orderBy(TASK_SEQ.asc()).fetch(mapper)
        }
    }

    fun getLatestNormalRecords(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        executeCount: Int,
        matrixContainerIds: List<String>
    ): List<BuildRecordTask> {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(EXECUTE_COUNT.lessOrEqual(executeCount))
            if (matrixContainerIds.isNotEmpty()) {
                conditions.add(CONTAINER_ID.notIn(matrixContainerIds))
            }
            // 获取每个最大执行次数
            val max = DSL.select(
                TASK_ID.`as`(KEY_TASK_ID),
                DSL.max(EXECUTE_COUNT).`as`(KEY_EXECUTE_COUNT)
            ).from(this).where(conditions).groupBy(TASK_ID)
            val result = dslContext.select(
                BUILD_ID, PROJECT_ID, PIPELINE_ID, RESOURCE_VERSION, STAGE_ID, CONTAINER_ID, TASK_ID,
                TASK_SEQ, EXECUTE_COUNT, TASK_VAR, CLASS_TYPE, ATOM_CODE, STATUS, ORIGIN_CLASS_TYPE,
                START_TIME, END_TIME, TIMESTAMPS, POST_INFO
            ).from(this).join(max).on(
                TASK_ID.eq(max.field(KEY_TASK_ID, String::class.java))
                    .and(EXECUTE_COUNT.eq(max.field(KEY_EXECUTE_COUNT, Int::class.java)))
            ).where(conditions).orderBy(TASK_SEQ.asc())
                .fetch()
            return result.map { record ->
                generateBuildRecordTask(record)
            }
        }
    }

    fun getLatestMatrixRecords(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        executeCount: Int,
        matrixContainerIds: List<String>
    ): List<BuildRecordTask> {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val conditions = BUILD_ID.eq(buildId)
                .and(PROJECT_ID.eq(projectId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .and(CONTAINER_ID.`in`(matrixContainerIds))
            val result = dslContext.select(
                BUILD_ID, PROJECT_ID, PIPELINE_ID, RESOURCE_VERSION, STAGE_ID, CONTAINER_ID, TASK_ID,
                TASK_SEQ, EXECUTE_COUNT, TASK_VAR, CLASS_TYPE, ATOM_CODE, STATUS, ORIGIN_CLASS_TYPE,
                START_TIME, END_TIME, TIMESTAMPS, POST_INFO
            ).from(this).where(conditions).orderBy(TASK_SEQ.asc()).fetch()
            return result.map { record ->
                generateBuildRecordTask(record)
            }
        }
    }

    private fun TPipelineBuildRecordTask.generateBuildRecordTask(
        record: Record18<String, String, String, Int, String,
            String, String, Int, Int, String, String, String,
            String, String, LocalDateTime, LocalDateTime, String, String>
    ) =
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
            taskVar = JsonUtil.to(
                record[TASK_VAR], object : TypeReference<MutableMap<String, Any>>() {}
            ),
            classType = record[CLASS_TYPE],
            atomCode = record[ATOM_CODE],
            status = record[STATUS],
            originClassType = record[ORIGIN_CLASS_TYPE],
            startTime = record[START_TIME],
            endTime = record[END_TIME],
            timestamps = record[TIMESTAMPS]?.let {
                JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
            } ?: mapOf(),
            elementPostInfo = record[POST_INFO]?.let {
                JsonUtil.to(it, object : TypeReference<ElementPostInfo>() {})
            }
        )

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
                    startTime = startTime,
                    endTime = endTime,
                    timestamps = timestamps?.let {
                        JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
                    } ?: mapOf(),
                    elementPostInfo = postInfo?.let {
                        JsonUtil.to(it, object : TypeReference<ElementPostInfo>() {})
                    }
                )
            }
        }
    }

    companion object {
        private val mapper = BuildRecordTaskJooqMapper()
    }
}
