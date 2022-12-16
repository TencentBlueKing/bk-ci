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
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.model.process.tables.TPipelineBuildRecordContainer
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordContainerRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Repository
@Suppress("LongParameterList")
class BuildRecordContainerDao {

    fun batchSave(dslContext: DSLContext, records: List<BuildRecordContainer>) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            records.forEach { record ->
                dslContext.insertInto(this)
                    .set(BUILD_ID, record.buildId)
                    .set(PROJECT_ID, record.projectId)
                    .set(PIPELINE_ID, record.pipelineId)
                    .set(RESOURCE_VERSION, record.resourceVersion)
                    .set(STAGE_ID, record.stageId)
                    .set(CONTAINER_ID, record.containerId)
                    .set(EXECUTE_COUNT, record.executeCount)
                    .set(CONTAINER_VAR, JsonUtil.toJson(record.containerVar, false))
                    .set(CONTAINER_TYPE, record.containerType)
                    .set(MATRIX_GROUP_FLAG, record.matrixGroupFlag)
                    .set(MATRIX_GROUP_ID, record.matrixGroupId)
                    .set(STATUS, record.status)
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
        containerId: String,
        executeCount: Int,
        containerVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val update = dslContext.update(this)
                .set(CONTAINER_VAR, JsonUtil.toJson(containerVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
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
        stageId: String?
    ): List<BuildRecordContainer> {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(EXECUTE_COUNT.eq(executeCount))
            stageId?.let { conditions.add(STAGE_ID.eq(stageId)) }
            return dslContext.selectFrom(this)
                .where(conditions).orderBy(CONTAINER_ID.asc()).fetch(mapper)
        }
    }

    fun getRecordContainerVar(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int
    ): Map<String, Any>? {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            return dslContext.select(CONTAINER_VAR)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(CONTAINER_VAR.eq(containerId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchOne(0, String::class.java)?.let {
                    JsonUtil.getObjectMapper().readValue(it) as Map<String, Any>
                }
        }
    }

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
                    matrixGroupFlag = matrixGroupFlag,
                    matrixGroupId = matrixGroupId,
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
