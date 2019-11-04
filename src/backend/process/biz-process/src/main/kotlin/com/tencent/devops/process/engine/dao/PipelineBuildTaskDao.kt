/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
import com.tencent.devops.process.pojo.ErrorType
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
                    CONTAINER_HASH_ID,
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
                        buildTask.containerHashId,
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
        with(T_PIPELINE_BUILD_TASK) {
            dslContext.batch(taskList.map { buildTask ->
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    STAGE_ID,
                    CONTAINER_ID,
                    TASK_NAME,
                    TASK_ID,
                    TASK_PARAMS,
                    TASK_TYPE,
                    TASK_ATOM,
                    START_TIME,
                    END_TIME,
                    STARTER,
                    APPROVER,
                    STATUS,
                    EXECUTE_COUNT,
                    TASK_SEQ,
                    SUB_BUILD_ID,
                    CONTAINER_TYPE,
                    ADDITIONAL_OPTIONS,
                    TOTAL_TIME,
                    ERROR_TYPE,
                    ERROR_CODE,
                    ERROR_MSG
                )
                    .values(
                        buildTask.projectId,
                        buildTask.pipelineId,
                        buildTask.buildId,
                        buildTask.stageId,
                        buildTask.containerId,
                        buildTask.taskName,
                        buildTask.taskId,
                        objectMapper.writeValueAsString(buildTask.taskParams),
                        buildTask.taskType,
                        buildTask.taskAtom,
                        buildTask.startTime,
                        buildTask.endTime,
                        buildTask.starter,
                        buildTask.approver,
                        buildTask.status.ordinal,
                        buildTask.executeCount,
                        buildTask.taskSeq,
                        buildTask.subBuildId,
                        buildTask.containerType,
                        objectMapper.writeValueAsString(buildTask.additionalOptions),
                        if (buildTask.endTime != null && buildTask.startTime != null) {
                            Duration.between(buildTask.startTime, buildTask.endTime).toMillis() / 1000
                        } else {
                            null
                        },
                        buildTask.errorType?.ordinal,
                        buildTask.errorCode,
                        buildTask.errorMsg
                    )
            }).execute()
        }
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
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                containerId = containerId,
                containerHashId = containerHashId,
                containerType = containerType,
                taskSeq = taskSeq,
                taskId = taskId,
                taskName = taskName,
                taskType = taskType,
                taskAtom = taskAtom,
                status = BuildStatus.values()[status],
                taskParams = JsonUtil.toMutableMapSkipEmpty(taskParams),
                additionalOptions = JsonUtil.toOrNull(additionalOptions, ElementAdditionalOptions::class.java),
                executeCount = executeCount ?: 1,
                starter = starter,
                approver = approver,
                subBuildId = subBuildId,
                startTime = startTime,
                endTime = endTime,
                errorType = if (errorType == null) null else ErrorType.values()[errorType],
                errorCode = errorCode,
                errorMsg = errorMsg
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
        buildStatus: BuildStatus,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null
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
            if (errorType != null) {
                update.set(ERROR_TYPE, errorType.ordinal)
                update.set(ERROR_CODE, errorCode)
                update.set(ERROR_MSG, errorMsg)
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

    fun updateTaskParam(
        dslContext: DSLContext,
        buildId: String,
        taskId: String,
        taskParam: String
    ): Int {
        with(T_PIPELINE_BUILD_TASK) {
            return dslContext.update(this)
                .set(TASK_PARAMS, taskParam)
                .where(BUILD_ID.eq(buildId))
                .and(TASK_ID.eq(taskId)).execute()
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
