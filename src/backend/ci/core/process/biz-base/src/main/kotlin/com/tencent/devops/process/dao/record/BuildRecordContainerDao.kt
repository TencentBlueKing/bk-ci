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
import com.tencent.devops.model.process.tables.TPipelineBuildRecordContainer
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordContainerRecord
import com.tencent.devops.process.pojo.KEY_CONTAINER_ID
import com.tencent.devops.process.pojo.KEY_EXECUTE_COUNT
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record15
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.jooq.util.mysql.MySQLDSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList")
class BuildRecordContainerDao {

    fun batchSave(dslContext: DSLContext, records: List<BuildRecordContainer>) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            dslContext.insertInto(
                this,
                BUILD_ID,
                PROJECT_ID,
                PIPELINE_ID,
                RESOURCE_VERSION,
                STAGE_ID,
                CONTAINER_ID,
                EXECUTE_COUNT,
                CONTAINER_VAR,
                CONTAINER_TYPE,
                MATRIX_GROUP_FLAG,
                MATRIX_GROUP_ID,
                STATUS,
                TIMESTAMPS
            ).also { insert ->
                records.forEach { record ->
                    insert.values(
                        record.buildId,
                        record.projectId,
                        record.pipelineId,
                        record.resourceVersion,
                        record.stageId,
                        record.containerId,
                        record.executeCount,
                        JsonUtil.toJson(record.containerVar, false),
                        record.containerType,
                        record.matrixGroupFlag,
                        record.matrixGroupId,
                        record.status,
                        JsonUtil.toJson(record.timestamps, false)
                    )
                }
            }.onDuplicateKeyUpdate()
                .set(STATUS, MySQLDSL.values(STATUS))
                .set(START_TIME, MySQLDSL.values(START_TIME))
                .set(END_TIME, MySQLDSL.values(END_TIME))
                .set(TIMESTAMPS, MySQLDSL.values(TIMESTAMPS))
                .set(CONTAINER_VAR, MySQLDSL.values(CONTAINER_VAR))
                .execute()
        }
    }

    fun updateRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int,
        containerVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val update = dslContext.update(this)
                .set(CONTAINER_VAR, JsonUtil.toJson(containerVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
            startTime?.let { update.set(START_TIME, startTime) }
            endTime?.let { update.set(END_TIME, endTime) }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(CONTAINER_ID.eq(containerId))
            ).execute()
        }
    }

    fun getRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int
    ): BuildRecordContainer? {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            return dslContext.selectFrom(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(BUILD_ID.eq(buildId))
                        .and(CONTAINER_ID.eq(containerId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchOne(mapper)
        }
    }

    fun getRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        stageId: String? = null,
        matrixGroupId: String? = null,
        buildStatusSet: Set<BuildStatus>? = null
    ): List<BuildRecordContainer> {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(EXECUTE_COUNT.eq(executeCount))
            stageId?.let { conditions.add(STAGE_ID.eq(stageId)) }
            matrixGroupId?.let { conditions.add(MATRIX_GROUP_ID.eq(matrixGroupId)) }
            buildStatusSet?.let { conditions.add(STATUS.`in`(it.map { status -> status.name })) }
            return dslContext.selectFrom(this)
                .where(conditions).orderBy(CONTAINER_ID.asc()).fetch(mapper)
        }
    }

    fun updateRecordStatus(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        stageId: String? = null
    ) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val update = dslContext.update(this)
                .set(STATUS, buildStatus.name)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                )
            stageId?.let { update.and(STAGE_ID.eq(stageId)) }
            update.execute()
        }
    }

    fun getLatestNormalRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): List<BuildRecordContainer> {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val conditions = BUILD_ID.eq(buildId)
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(EXECUTE_COUNT.lessOrEqual(executeCount))
                .and(MATRIX_GROUP_ID.isNull)
            // 获取每个最大执行次数
            val max = DSL.select(
                CONTAINER_ID.`as`(KEY_CONTAINER_ID),
                DSL.max(EXECUTE_COUNT).`as`(KEY_EXECUTE_COUNT)
            ).from(this).where(conditions).groupBy(CONTAINER_ID)
            val result = dslContext.select(
                BUILD_ID, PROJECT_ID, PIPELINE_ID, RESOURCE_VERSION, STAGE_ID, CONTAINER_ID,
                CONTAINER_VAR, EXECUTE_COUNT, CONTAINER_TYPE, STATUS, MATRIX_GROUP_FLAG,
                MATRIX_GROUP_ID, START_TIME, END_TIME, TIMESTAMPS
            ).from(this).join(max).on(
                CONTAINER_ID.eq(max.field(KEY_CONTAINER_ID, String::class.java))
                    .and(EXECUTE_COUNT.eq(max.field(KEY_EXECUTE_COUNT, Int::class.java)))
            ).where(conditions).orderBy(CONTAINER_ID.asc())
                .fetch()
            return result.map { record ->
                generateBuildRecordContainer(record)
            }
        }
    }

    fun getLatestMatrixRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): List<BuildRecordContainer> {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val conditions = BUILD_ID.eq(buildId)
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .and(MATRIX_GROUP_ID.isNotNull)
            val result = dslContext.select(
                BUILD_ID, PROJECT_ID, PIPELINE_ID, RESOURCE_VERSION, STAGE_ID, CONTAINER_ID,
                CONTAINER_VAR, EXECUTE_COUNT, CONTAINER_TYPE, STATUS, MATRIX_GROUP_FLAG,
                MATRIX_GROUP_ID, START_TIME, END_TIME, TIMESTAMPS
            ).from(this).where(conditions).orderBy(CONTAINER_ID.asc()).fetch()
            return result.map { record ->
                generateBuildRecordContainer(record)
            }
        }
    }

    private fun TPipelineBuildRecordContainer.generateBuildRecordContainer(
        record: Record15<String, String, String, Int,
            String, String, String, Int, String, String, Boolean, String, LocalDateTime, LocalDateTime, String>
    ) =
        BuildRecordContainer(
            buildId = record[BUILD_ID],
            projectId = record[PROJECT_ID],
            pipelineId = record[PIPELINE_ID],
            resourceVersion = record[RESOURCE_VERSION],
            stageId = record[STAGE_ID],
            containerId = record[CONTAINER_ID],
            executeCount = record[EXECUTE_COUNT],
            status = record[STATUS],
            containerVar = JsonUtil.to(
                record[CONTAINER_VAR], object : TypeReference<MutableMap<String, Any>>() {}
            ),
            containerType = record[CONTAINER_TYPE],
            matrixGroupFlag = record[MATRIX_GROUP_FLAG],
            matrixGroupId = record[MATRIX_GROUP_ID],
            startTime = record[START_TIME],
            endTime = record[END_TIME],
            timestamps = record[TIMESTAMPS]?.let {
                JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
            } ?: mapOf()
        )

    class BuildRecordContainerJooqMapper : RecordMapper<TPipelineBuildRecordContainerRecord, BuildRecordContainer> {
        override fun map(record: TPipelineBuildRecordContainerRecord?): BuildRecordContainer? {
            return record?.run {
                BuildRecordContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    resourceVersion = resourceVersion,
                    executeCount = executeCount,
                    stageId = stageId,
                    containerId = containerId,
                    containerVar = JsonUtil.to(
                        containerVar, object : TypeReference<Map<String, Any>>() {}
                    ).toMutableMap(),
                    containerType = containerType,
                    status = status,
                    containPostTaskFlag = containPostTask,
                    matrixGroupFlag = matrixGroupFlag,
                    matrixGroupId = matrixGroupId,
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
        private val mapper = BuildRecordContainerJooqMapper()
    }
}
