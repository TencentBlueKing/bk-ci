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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.tables.TPipelineBuildRecordPipeline
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordPipelineRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordPipeline
import com.tencent.devops.process.pojo.pipeline.record.time.BuildRecordTimeCost
import com.tencent.devops.process.pojo.pipeline.record.time.BuildRecordTimeStamp
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildRecordPipelineDao {

    fun createRecord(dslContext: DSLContext, record: BuildRecordPipeline) {
        with(TPipelineBuildRecordPipeline.T_PIPELINE_BUILD_RECORD_PIPELINE) {
            dslContext.insertInto(this)
                .set(BUILD_ID, record.buildId)
                .set(PROJECT_ID, record.projectId)
                .set(PIPELINE_ID, record.buildId)
                .set(RESOURCE_VERSION, record.resourceVersion)
                .set(BUILD_NUM, record.buildNum)
                .set(EXECUTE_COUNT, record.buildNum)
                .set(START_USER, record.startUser)
                .set(TRIGGER, record.trigger)
                .set(PIPELINE_VAR, JsonUtil.toJson(record.pipelineVar, false))
                .set(CANCEL_USER, record.cancelUser)
                .set(START_TIME, record.startTime)
                .set(END_TIME, record.endTime)
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
        pipelineVar: Map<String, Any>,
        cancelUser: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        timestamps: List<BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordPipeline.T_PIPELINE_BUILD_RECORD_PIPELINE) {
            val update = dslContext.update(this)
                .set(PIPELINE_VAR, JsonUtil.toJson(pipelineVar, false))
            cancelUser?.let { update.set(CANCEL_USER, cancelUser) }
            startTime?.let { update.set(START_TIME, startTime) }
            endTime?.let { update.set(END_TIME, endTime) }
            timestamps?.let { update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false)) }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
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
    ): BuildRecordPipeline {
        with(TPipelineBuildRecordPipeline.T_PIPELINE_BUILD_RECORD_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchSingle(mapper)
        }
    }

    class BuildRecordPipelineJooqMapper : RecordMapper<TPipelineBuildRecordPipelineRecord, BuildRecordPipeline> {
        override fun map(record: TPipelineBuildRecordPipelineRecord?): BuildRecordPipeline? {
            return record?.run {
                BuildRecordPipeline(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    resourceVersion = resourceVersion,
                    executeCount = executeCount,
                    buildNum = buildNum,
                    pipelineVar = JsonUtil.getObjectMapper().readValue(pipelineVar) as MutableMap<String, Any>,
                    startUser = startUser,
                    trigger = trigger,
                    cancelUser = cancelUser,
                    startTime = startTime,
                    endTime = endTime,
                    timestamps = timestamps?.let {
                        JsonUtil.getObjectMapper().readValue(it) as List<BuildRecordTimeStamp>
                    } ?: emptyList(),
                    timeCost = timeCost?.let {
                        JsonUtil.getObjectMapper().readValue(it, BuildRecordTimeCost::class.java)
                    }
                )
            }
        }
    }

    companion object {
        private val mapper = BuildRecordPipelineJooqMapper()
    }
}
