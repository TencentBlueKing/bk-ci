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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_TASK
import com.tencent.devops.model.process.tables.TPipelineBuildTask
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.UpdateTaskInfo
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record3
import org.jooq.RecordMapper
import org.jooq.Result
import org.jooq.impl.DSL.count
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions", "LongParameterList")
@Repository
class PipelineBuildTaskDao {

    fun create(dslContext: DSLContext, buildTask: PipelineBuildTask) {

        val count =
            with(T_PIPELINE_BUILD_TASK) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    STAGE_ID,
                    CONTAINER_TYPE,
                    CONTAINER_ID,
                    CONTAINER_HASH_ID,
                    TASK_SEQ,
                    TASK_ID,
                    STEP_ID,
                    TASK_TYPE,
                    TASK_ATOM,
                    SUB_PROJECT_ID,
                    SUB_BUILD_ID,
                    STATUS,
                    STARTER,
                    TASK_PARAMS,
                    START_TIME,
                    END_TIME,
                    APPROVER,
                    ADDITIONAL_OPTIONS,
                    ATOM_CODE
                )
                    .values(
                        buildTask.projectId,
                        buildTask.pipelineId,
                        buildTask.buildId,
                        buildTask.stageId,
                        buildTask.containerType,
                        buildTask.containerId,
                        buildTask.containerHashId,
                        buildTask.taskSeq,
                        buildTask.taskId,
                        buildTask.stepId,
                        buildTask.taskType,
                        buildTask.taskAtom,
                        buildTask.subProjectId,
                        buildTask.subBuildId,
                        buildTask.status.ordinal,
                        buildTask.starter,
                        JsonUtil.toJson(buildTask.taskParams, formatted = false),
                        buildTask.startTime,
                        buildTask.endTime,
                        buildTask.approver,
                        buildTask.additionalOptions?.let { self -> JsonUtil.toJson(self, formatted = false) },
                        buildTask.atomCode
                    )
                    .execute()
            }
        logger.info("save the buildTask=$buildTask, result=${count == 1}")
    }

    fun batchSave(dslContext: DSLContext, taskList: Collection<PipelineBuildTask>) {
        with(T_PIPELINE_BUILD_TASK) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                STAGE_ID,
                CONTAINER_ID,
                TASK_NAME,
                TASK_ID,
                STEP_ID,
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
                SUB_PROJECT_ID,
                SUB_BUILD_ID,
                CONTAINER_TYPE,
                ADDITIONAL_OPTIONS,
                TOTAL_TIME,
                ERROR_TYPE,
                ERROR_CODE,
                ERROR_MSG,
                CONTAINER_HASH_ID,
                ATOM_CODE
            ).also { insert ->
                taskList.forEach {
                    insert.values(
                        it.projectId,
                        it.pipelineId,
                        it.buildId,
                        it.stageId,
                        it.containerId,
                        it.taskName,
                        it.taskId,
                        it.stepId,
                        JsonUtil.toJson(it.taskParams, formatted = false),
                        it.taskType,
                        it.taskAtom,
                        it.startTime,
                        it.endTime,
                        it.starter,
                        it.approver,
                        it.status.ordinal,
                        it.executeCount,
                        it.taskSeq,
                        it.subProjectId,
                        it.subBuildId,
                        it.containerType,
                        it.additionalOptions?.let { self -> JsonUtil.toJson(self, formatted = false) },
                        if (it.endTime != null && it.startTime != null) {
                            TimeUnit.MILLISECONDS.toSeconds(Duration.between(it.startTime, it.endTime).toMillis())
                        } else {
                            null
                        },
                        it.errorType?.ordinal,
                        it.errorCode,
                        it.errorMsg?.coerceAtMaxLength(PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX),
                        it.containerHashId,
                        it.atomCode
                    )
                }
            }.execute()
        }
    }

    fun batchUpdate(dslContext: DSLContext, taskList: List<PipelineBuildTask>) {
        with(T_PIPELINE_BUILD_TASK) {
            taskList.forEach {
                dslContext.update(this)
                    .set(PIPELINE_ID, it.pipelineId)
                    .set(STAGE_ID, it.stageId)
                    .set(CONTAINER_ID, it.containerId)
                    .set(TASK_NAME, it.taskName)
                    .set(STEP_ID, it.stepId)
                    .set(TASK_PARAMS, JsonUtil.toJson(it.taskParams, formatted = false))
                    .set(TASK_TYPE, it.taskType)
                    .set(TASK_ATOM, it.taskAtom)
                    .set(START_TIME, it.startTime)
                    .set(END_TIME, it.endTime)
                    .set(STARTER, it.starter)
                    .set(APPROVER, it.approver)
                    .set(STATUS, it.status.ordinal)
                    .set(EXECUTE_COUNT, it.executeCount)
                    .set(TASK_SEQ, it.taskSeq)
                    .set(SUB_PROJECT_ID, it.subProjectId)
                    .set(SUB_BUILD_ID, it.subBuildId)
                    .set(CONTAINER_TYPE, it.containerType)
                    .set(
                        ADDITIONAL_OPTIONS,
                        it.additionalOptions?.let { self -> JsonUtil.toJson(self, formatted = false) }
                    )
                    .set(TOTAL_TIME, it.totalTime)
                    .set(ERROR_TYPE, it.errorType?.ordinal)
                    .set(ERROR_MSG, it.errorMsg?.coerceAtMaxLength(PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX))
                    .set(ERROR_CODE, it.errorCode)
                    .set(CONTAINER_HASH_ID, it.containerHashId)
                    .set(ATOM_CODE, it.atomCode)
                    .where(BUILD_ID.eq(it.buildId).and(TASK_ID.eq(it.taskId)).and(PROJECT_ID.eq(it.projectId)))
                    .execute()
            }
        }
    }

    fun get(dslContext: DSLContext, projectId: String, buildId: String, taskId: String?): PipelineBuildTask? {
        return with(T_PIPELINE_BUILD_TASK) {

            val where = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            if (taskId != null) {
                where.and(TASK_ID.eq(taskId))
            }
            where.fetchAny(mapper)
        }
    }

    fun getTaskStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        taskId: String?
    ): Record1<Int>? {

        return with(T_PIPELINE_BUILD_TASK) {
            val where = dslContext.select(STATUS)
                .from(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            if (taskId != null) {
                where.and(TASK_ID.eq(taskId))
            }
            where.fetchAny()
        }
    }

    fun getByBuildId(dslContext: DSLContext, projectId: String, buildId: String): Collection<PipelineBuildTask> {
        return with(T_PIPELINE_BUILD_TASK) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
                .orderBy(TASK_SEQ.asc()).fetch(mapper)
        }
    }

    fun getTasksInCondition(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        containerId: String?,
        statusSet: Collection<BuildStatus>?
    ): List<PipelineBuildTask> {
        return with(T_PIPELINE_BUILD_TASK) {
            val where = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            containerId?.let { where.and(CONTAINER_ID.eq(containerId)) }
            if (!statusSet.isNullOrEmpty()) {
                where.and(STATUS.`in`(statusSet.map { it.ordinal }))
            }
            where.orderBy(TASK_SEQ.asc()).fetch(mapper)
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

    fun deleteBuildTasksByContainerSeqId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String
    ): Int {
        return with(T_PIPELINE_BUILD_TASK) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .and(CONTAINER_ID.eq(containerId))
                .execute()
        }
    }

    fun updateSubBuildId(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        taskId: String,
        subBuildId: String,
        subProjectId: String
    ): Int {
        return with(T_PIPELINE_BUILD_TASK) {
            dslContext.update(this)
                .set(SUB_BUILD_ID, subBuildId)
                .set(SUB_PROJECT_ID, subProjectId)
                .where(BUILD_ID.eq(buildId))
                .and(TASK_ID.eq(taskId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun updateTaskInfo(dslContext: DSLContext, updateTaskInfo: UpdateTaskInfo) {
        with(T_PIPELINE_BUILD_TASK) {
            val projectId = updateTaskInfo.projectId
            val buildId = updateTaskInfo.buildId
            val taskId = updateTaskInfo.taskId
            val baseStep = dslContext.update(this)
                .set(STATUS, updateTaskInfo.taskStatus.ordinal)
                .set(EXECUTE_COUNT, updateTaskInfo.executeCount)
            updateTaskInfo.starter?.let { baseStep.set(STARTER, it) }
            updateTaskInfo.approver?.let { baseStep.set(APPROVER, it) }
            updateTaskInfo.startTime?.let { baseStep.set(START_TIME, it) }
            updateTaskInfo.endTime?.let { baseStep.set(END_TIME, it) }
            updateTaskInfo.totalTime?.let { baseStep.set(TOTAL_TIME, it) }
            updateTaskInfo.errorType?.let { baseStep.set(ERROR_TYPE, it.num) }
            updateTaskInfo.errorCode?.let { baseStep.set(ERROR_CODE, it) }
            updateTaskInfo.errorMsg?.let {
                baseStep.set(ERROR_MSG, it.coerceAtMaxLength(PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX))
            }
            updateTaskInfo.additionalOptions?.let {
                baseStep.set(ADDITIONAL_OPTIONS, JsonUtil.toJson(it, formatted = false))
            }
            updateTaskInfo.taskParams?.let { baseStep.set(TASK_PARAMS, JsonUtil.toJson(it, formatted = false)) }
            updateTaskInfo.platformCode?.let { baseStep.set(PLATFORM_CODE, it) }
            updateTaskInfo.platformErrorCode?.let { baseStep.set(PLATFORM_ERROR_CODE, it) }
            baseStep.where(BUILD_ID.eq(buildId)).and(TASK_ID.eq(taskId)).and(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun setTaskErrorInfo(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        taskId: String,
        errorType: ErrorType,
        errorCode: Int,
        errorMsg: String
    ) {
        with(T_PIPELINE_BUILD_TASK) {
            dslContext.update(this)
                .set(ERROR_TYPE, errorType.num)
                .set(ERROR_CODE, errorCode)
                .set(ERROR_MSG, errorMsg.coerceAtMaxLength(PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX))
                .where(BUILD_ID.eq(buildId)).and(TASK_ID.eq(taskId)).and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun countGroupByBuildId(
        dslContext: DSLContext,
        projectId: String,
        buildIds: Collection<String>
    ): Result<Record3<String/*BUILD_ID*/, Int/*STATUS*/, Int/*COUNT*/>> {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            return dslContext.select(BUILD_ID, STATUS, count())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.`in`(buildIds))
                .groupBy(BUILD_ID, STATUS)
                .fetch()
        }
    }

    fun updateTaskParam(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        taskId: String,
        taskParam: String
    ): Int {
        with(T_PIPELINE_BUILD_TASK) {
            return dslContext.update(this)
                .set(TASK_PARAMS, taskParam)
                .where(BUILD_ID.eq(buildId))
                .and(TASK_ID.eq(taskId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun list(dslContext: DSLContext, projectId: String, buildId: String): Result<TPipelineBuildTaskRecord> {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
                .fetch()
        }
    }

    class PipelineBuildTaskJooqMapper : RecordMapper<TPipelineBuildTaskRecord, PipelineBuildTask> {
        override fun map(record: TPipelineBuildTaskRecord?): PipelineBuildTask? {
            return record?.run {
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
                    stepId = stepId,
                    taskName = taskName,
                    taskType = taskType,
                    taskAtom = taskAtom,
                    status = BuildStatus.values()[status],
                    taskParams = JsonUtil.toMutableMap(taskParams),
                    additionalOptions = JsonUtil.toOrNull(additionalOptions, ElementAdditionalOptions::class.java),
                    executeCount = executeCount ?: 1,
                    starter = starter,
                    approver = approver,
                    subProjectId = subProjectId,
                    subBuildId = subBuildId,
                    startTime = startTime,
                    endTime = endTime,
                    totalTime = totalTime,
                    errorType = if (errorType == null) null else ErrorType.values()[errorType],
                    errorCode = errorCode,
                    errorMsg = errorMsg,
                    atomCode = atomCode
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineBuildTaskJooqMapper()
        private val logger = LoggerFactory.getLogger(PipelineBuildTaskDao::class.java)
    }
}
