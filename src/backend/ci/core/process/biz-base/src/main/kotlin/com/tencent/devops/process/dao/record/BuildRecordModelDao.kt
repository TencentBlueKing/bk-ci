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
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.model.process.tables.TPipelineBuildRecordModel
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordModelRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.jooq.impl.DSL.select
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList")
class BuildRecordModelDao {

    fun createRecord(dslContext: DSLContext, record: BuildRecordModel) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.insertInto(this)
                .set(BUILD_ID, record.buildId)
                .set(PROJECT_ID, record.projectId)
                .set(PIPELINE_ID, record.pipelineId)
                .set(RESOURCE_VERSION, record.resourceVersion)
                .set(BUILD_NUM, record.buildNum)
                .set(EXECUTE_COUNT, record.executeCount)
                .set(START_USER, record.startUser)
                .set(START_TYPE, record.startType)
                .set(MODEL_VAR, JsonUtil.toJson(record.modelVar, false))
                .set(STATUS, record.status)
                .set(ERROR_INFO, record.errorInfoList?.let { JsonUtil.toJson(it, false) })
                .set(CANCEL_USER, record.cancelUser)
                .set(TIMESTAMPS, JsonUtil.toJson(record.timestamps, false))
                .execute()
        }
    }

    fun updateRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus?,
        modelVar: Map<String, Any>,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        errorInfoList: List<ErrorInfo>?,
        cancelUser: String?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            val update = dslContext.update(this)
                .set(MODEL_VAR, JsonUtil.toJson(modelVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            cancelUser?.let { update.set(CANCEL_USER, cancelUser) }
            startTime?.let { update.set(START_TIME, startTime) }
            endTime?.let { update.set(END_TIME, endTime) }
            timestamps?.let {
                update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false))
            }
            errorInfoList?.let {
                update.set(ERROR_INFO, JsonUtil.toJson(errorInfoList, false))
            }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(executeCount))
            ).execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        buildStatus: BuildStatus,
        executeCount: Int
    ) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.update(this).set(STATUS, buildStatus.name)
                .where(
                    PROJECT_ID.eq(projectId)
                        .and(BUILD_ID.eq(buildId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).execute()
        }
    }

    fun getRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): BuildRecordModel? {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            return dslContext.selectFrom(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchAny(mapper)
        }
    }

    fun getRecordStartUserList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<String> {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            return dslContext.select(START_USER).from(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                ).orderBy(EXECUTE_COUNT.desc()).fetch(START_USER)
        }
    }

    class BuildRecordPipelineJooqMapper : RecordMapper<TPipelineBuildRecordModelRecord, BuildRecordModel> {
        override fun map(record: TPipelineBuildRecordModelRecord?): BuildRecordModel? {
            return record?.run {
                BuildRecordModel(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    resourceVersion = resourceVersion,
                    executeCount = executeCount,
                    buildNum = buildNum,
                    modelVar = JsonUtil.to(
                        modelVar, object : TypeReference<Map<String, Any>>() {}
                    ).toMutableMap(),
                    queueTime = queueTime,
                    startTime = startTime,
                    endTime = endTime,
                    startUser = startUser,
                    startType = startType,
                    status = status,
                    cancelUser = cancelUser,
                    errorInfoList = errorInfo?.let {
                        JsonUtil.to(it, object : TypeReference<List<ErrorInfo>>() {})
                    },
                    timestamps = timestamps?.let {
                        JsonUtil.to(it, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp>>() {})
                    } ?: mapOf()
                )
            }
        }
    }

    fun updateBuildCancelUser(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        executeCount: Int,
        cancelUser: String
    ) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.update(this)
                .set(CANCEL_USER, cancelUser)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .execute()
        }
    }

    companion object {
        private val mapper = BuildRecordPipelineJooqMapper()
    }
}
