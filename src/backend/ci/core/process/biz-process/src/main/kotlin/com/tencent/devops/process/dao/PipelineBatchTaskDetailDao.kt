package com.tencent.devops.process.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_BATCH_TASK_DETAIL
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBatchTaskDetailDao {

    fun batchCreate(
        dslContext: DSLContext,
        details: List<PipelineBatchTaskDetailInfo>
    ): IntArray {
        if (details.isEmpty()) {
            return intArrayOf()
        }
        val queries = with(T_PIPELINE_BATCH_TASK_DETAIL) {
            details.map { detail ->
                dslContext.insertInto(
                    this,
                    TASK_ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIPELINE_NAME,
                    STATUS,
                    ERROR_MESSAGE,
                    START_TIME,
                    END_TIME
                ).values(
                    detail.taskId,
                    detail.projectId,
                    detail.pipelineId,
                    detail.pipelineName,
                    detail.status.name,
                    detail.errorMessage,
                    detail.startTime,
                    detail.endTime
                )
            }
        }
        return dslContext.batch(queries).execute()
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ): Long {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectCount()
                .from(this)
                .where(buildTaskConditions(projectId = projectId, taskId = taskId))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        offset: Int,
        limit: Int
    ): List<PipelineBatchTaskDetailInfo> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectFrom(this)
                .where(buildTaskConditions(projectId = projectId, taskId = taskId))
                .orderBy(PIPELINE_NAME.asc(), PIPELINE_ID.asc())
                .limit(offset, limit)
                .fetch()
                .map(::convert)
        }
    }

    fun listByTaskId(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ): List<PipelineBatchTaskDetailInfo> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectFrom(this)
                .where(buildTaskConditions(projectId = projectId, taskId = taskId))
                .fetch()
                .map(::convert)
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineId: String,
        status: PipelineBatchTaskStatus,
        errorMessage: String? = null
    ): Int {
        val now = LocalDateTime.now()
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .set(ERROR_MESSAGE, errorMessage)
                .set(END_TIME, now)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun markExecuting(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ): Int {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.update(this)
                .set(STATUS, PipelineBatchTaskStatus.EXECUTING.name)
                .set(START_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .execute()
        }
    }

    private fun buildTaskConditions(projectId: String, taskId: String): List<Condition> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            listOf(PROJECT_ID.eq(projectId), TASK_ID.eq(taskId))
        }
    }

    private fun convert(record: Record): PipelineBatchTaskDetailInfo {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            PipelineBatchTaskDetailInfo(
                taskId = record.get(TASK_ID),
                projectId = record.get(PROJECT_ID),
                pipelineId = record.get(PIPELINE_ID),
                pipelineName = record.get(PIPELINE_NAME),
                status = PipelineBatchTaskStatus.valueOf(record.get(STATUS)),
                errorMessage = record.get(ERROR_MESSAGE),
                startTime = record.get(START_TIME),
                endTime = record.get(END_TIME),
                createTime = record.get(CREATE_TIME),
                updateTime = record.get(UPDATE_TIME)
            )
        }
    }
}
