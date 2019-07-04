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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_TASK
import com.tencent.devops.model.process.tables.TPipelineBuildTask
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime

@Repository
class PipelineBuildTaskDao @Autowired constructor(private val objectMapper: ObjectMapper) {

    fun create(
        dslContext: DSLContext,
        buildTask: PipelineBuildTask
    ) {

        val count =
            with(T_PIPELINE_BUILD_TASK) {
                dslContext.insertInto(
                    this,
                    PIPELINE_ID,
                    BUILD_ID,
                    STAGE_ID,
                    CONTAINER_TYPE,
                    CONTAINER_ID,
                    TASK_SEQ,
                    TASK_ID,
                    TASK_TYPE,
                    TASK_ATOM,
                    SUB_BUILD_ID,
                    STATUS,
                    STARTER,
                    TASK_PARAMS,
                    START_TIME,
                    END_TIME,
                    APPROVER,
                    ADDITIONAL_OPTIONS
                )
                    .values(
                        buildTask.pipelineId,
                        buildTask.buildId,
                        buildTask.stageId,
                        buildTask.containerType,
                        buildTask.containerId,
                        buildTask.taskSeq,
                        buildTask.taskId,
                        buildTask.taskType,
                        buildTask.taskAtom,
                        buildTask.subBuildId,
                        buildTask.status.ordinal,
                        buildTask.starter,
                        objectMapper.writeValueAsString(buildTask.taskParams),
                        buildTask.startTime,
                        buildTask.endTime,
                        buildTask.approver,
                        objectMapper.writeValueAsString(buildTask.additionalOptions)
                    )
                    .execute()
            }
        logger.info("save the buildTask=$buildTask, result=${count == 1}")
    }

    fun batchSave(dslContext: DSLContext, taskList: Collection<PipelineBuildTask>) {
        val records = mutableListOf<TPipelineBuildTaskRecord>()
        taskList.forEach {
            with(it) {
                records.add(
                    TPipelineBuildTaskRecord(
                        pipelineId, projectId, buildId, stageId, containerId, taskName, taskId,
                        objectMapper.writeValueAsString(taskParams), taskType, taskAtom, startTime, endTime,
                        starter, approver, status.ordinal, executeCount, taskSeq, subBuildId, containerType,
                        objectMapper.writeValueAsString(additionalOptions),
                        if (endTime != null && startTime != null) {
                            Duration.between(startTime, endTime).toMillis() / 1000
                        } else null
                    )
                )
            }
        }
        dslContext.batchStore(records).execute()
    }

    fun get(
        dslContext: DSLContext,
        buildId: String,
        taskId: String?
    ): TPipelineBuildTaskRecord? {

        return with(T_PIPELINE_BUILD_TASK) {

            val where = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId))
            if (taskId != null) {
                where.and(TASK_ID.eq(taskId))
            }
            where.fetchAny()
        }
    }

    fun listByStatus(
        dslContext: DSLContext,
        buildId: String,
        containerId: String?,
        statusSet: Collection<BuildStatus>?
    ): List<TPipelineBuildTaskRecord> {
        return with(T_PIPELINE_BUILD_TASK) {
            val where = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
            if (!containerId.isNullOrBlank())
                where.and(CONTAINER_ID.eq(containerId))
            if (statusSet != null && statusSet.isNotEmpty()) {
                val statusIntSet = mutableSetOf<Int>()
                if (statusSet.isNotEmpty()) {
                    statusSet.forEach {
                        statusIntSet.add(it.ordinal)
                    }
                }
                where.and(STATUS.`in`(statusIntSet))
            }
            where.orderBy(TASK_SEQ.asc()).fetch()
        }
    }

    fun getByBuildId(dslContext: DSLContext, buildId: String): Collection<TPipelineBuildTaskRecord> {
        return with(T_PIPELINE_BUILD_TASK) {
            dslContext.selectFrom(this).where(BUILD_ID.eq(buildId)).orderBy(TASK_SEQ.asc()).fetch()
        }
    }

    fun deletePipelineBuildTasks(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(T_PIPELINE_BUILD_TASK) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun convert(tPipelineBuildTaskRecord: TPipelineBuildTaskRecord): PipelineBuildTask? {
        return with(tPipelineBuildTaskRecord) {
            PipelineBuildTask(
                projectId,
                pipelineId,
                buildId,
                stageId,
                containerId,
                containerType,
                taskSeq,
                taskId,
                taskName,
                taskType,
                taskAtom,
                BuildStatus.values()[status],
                JsonUtil.toMutableMapSkipEmpty(taskParams),
                JsonUtil.toOrNull(additionalOptions, ElementAdditionalOptions::class.java),
                executeCount ?: 1,
                starter,
                approver,
                subBuildId,
                startTime,
                endTime
            )
        }
    }

    fun updateSubBuildId(dslContext: DSLContext, buildId: String, taskId: String, subBuildId: String): Int {
        return with(T_PIPELINE_BUILD_TASK) {
            dslContext.update(this)
                .set(SUB_BUILD_ID, subBuildId)
                .where(BUILD_ID.eq(buildId))
                .and(TASK_ID.eq(taskId)).execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        taskId: String,
        userId: String?,
        buildStatus: BuildStatus
    ) {
        with(T_PIPELINE_BUILD_TASK) {
            val update = dslContext.update(this).set(STATUS, buildStatus.ordinal)
            // 根据状态来设置字段
            if (BuildStatus.isFinish(buildStatus)) {
                update.set(END_TIME, LocalDateTime.now())

                if (BuildStatus.isReview(buildStatus) && !userId.isNullOrBlank()) {
                    update.set(APPROVER, userId)
                }
            } else if (BuildStatus.isRunning(buildStatus)) {
                update.set(START_TIME, LocalDateTime.now())
                if (!userId.isNullOrBlank())
                    update.set(STARTER, userId)
            }
            update.where(BUILD_ID.eq(buildId)).and(TASK_ID.eq(taskId)).execute()

            if (BuildStatus.isFinish(buildStatus)) {
                val record = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId)).and(TASK_ID.eq(taskId)).fetchOne()
                val totalTime = if (record.startTime == null || record.endTime == null) {
                    0
                } else {
                    Duration.between(record.startTime, record.endTime).toMillis() / 1000
                }
                dslContext.update(this)
                    .set(TOTAL_TIME, (record.totalTime ?: 0) + totalTime)
                    .where(BUILD_ID.eq(buildId)).and(TASK_ID.eq(taskId)).execute()
            }
        }
    }

    fun list(dslContext: DSLContext, buildId: String): Result<TPipelineBuildTaskRecord> {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetch()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildTaskDao::class.java)
    }
}
