package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_BATCH_TASK
import com.tencent.devops.model.process.tables.records.TPipelineBatchTaskRecord
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStep
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBatchTaskDao {

    fun create(
        dslContext: DSLContext,
        taskId: String,
        projectId: String,
        taskName: String?,
        taskType: PipelineBatchTaskType,
        taskParam: String?,
        status: PipelineBatchTaskStatus,
        step: PipelineBatchTaskStep,
        totalCount: Int,
        subPipelineCount: Int,
        pacCount: Int,
        creator: String
    ): Int {
        return with(T_PIPELINE_BATCH_TASK) {
            dslContext.insertInto(
                this,
                TASK_ID,
                PROJECT_ID,
                TASK_NAME,
                TASK_TYPE,
                TASK_PARAM,
                TASK_SUMMARY,
                STATUS,
                STEP,
                TOTAL_COUNT,
                SUB_PIPELINE_COUNT,
                PAC_COUNT,
                SUCCESS_COUNT,
                FAILED_COUNT,
                CREATOR,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                taskId,
                projectId,
                taskName,
                taskType.name,
                taskParam,
                null,
                status.name,
                step.name,
                totalCount,
                subPipelineCount,
                pacCount,
                0,
                0,
                creator,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ): PipelineBatchTask? {
        return with(T_PIPELINE_BATCH_TASK) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .fetchOne()
                ?.let(::convert)
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        type: PipelineBatchTaskType?,
        status: PipelineBatchTaskStatus?,
        creator: String?
    ): Long {
        return with(T_PIPELINE_BATCH_TASK) {
            dslContext.selectCount()
                .from(this)
                .where(
                    buildConditions(
                        projectId = projectId,
                        type = type,
                        status = status,
                        creator = creator
                    )
                )
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        type: PipelineBatchTaskType?,
        status: PipelineBatchTaskStatus?,
        creator: String?,
        offset: Int,
        limit: Int
    ): List<PipelineBatchTask> {
        return with(T_PIPELINE_BATCH_TASK) {
            dslContext.selectFrom(this)
                .where(
                    buildConditions(
                        projectId = projectId,
                        type = type,
                        status = status,
                        creator = creator
                    )
                )
                .orderBy(CREATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
                .map(::convert)
        }
    }

    fun update(
        dslContext: DSLContext,
        update: PipelineBatchTaskUpdate
    ): Int {
        return with(T_PIPELINE_BATCH_TASK) {
            val query = dslContext.update(this)
            if (update.taskName != null) {
                query.set(TASK_NAME, update.taskName)
            }
            if (update.taskParam != null) {
                query.set(TASK_PARAM, update.taskParam)
            }
            if (update.taskSummary != null) {
                query.set(TASK_SUMMARY, update.taskSummary)
            }
            if (update.status != null) {
                query.set(STATUS, update.status!!.name)
            }
            if (update.subPipelineCount != null) {
                query.set(SUB_PIPELINE_COUNT, update.subPipelineCount)
            }
            if (update.pacCount != null) {
                query.set(PAC_COUNT, update.pacCount)
            }
            if (update.successCount != null) {
                query.set(SUCCESS_COUNT, update.successCount)
            }
            if (update.failedCount != null) {
                query.set(FAILED_COUNT, update.failedCount)
            }
            if (update.step != null) {
                query.set(STEP, update.step!!.name)
            }
            query.set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        status: PipelineBatchTaskStatus
    ): Int {
        return with(T_PIPELINE_BATCH_TASK) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .execute()
        }
    }

    private fun buildConditions(
        projectId: String,
        type: PipelineBatchTaskType?,
        status: PipelineBatchTaskStatus?,
        creator: String?
    ): List<Condition> {
        return with(T_PIPELINE_BATCH_TASK) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(STATUS.ne(PipelineBatchTaskStatus.DELETED.name))
            if (type != null) {
                conditions.add(TASK_TYPE.eq(type.name))
            }
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            if (!creator.isNullOrBlank()) {
                conditions.add(CREATOR.eq(creator))
            }
            conditions
        }
    }

    private fun convert(record: TPipelineBatchTaskRecord): PipelineBatchTask {
        return with(record) {
            PipelineBatchTask(
                taskId = taskId,
                projectId = projectId,
                taskName = taskName,
                taskType = PipelineBatchTaskType.valueOf(taskType),
                taskParam = taskParam,
                taskSummary = taskSummary,
                status = PipelineBatchTaskStatus.valueOf(status),
                step = PipelineBatchTaskStep.valueOf(step),
                totalCount = totalCount,
                subPipelineCount = subPipelineCount,
                pacCount = pacCount,
                successCount = successCount,
                failedCount = failedCount,
                creator = creator,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}
