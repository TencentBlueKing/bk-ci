package com.tencent.devops.process.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_BATCH_TASK
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStep
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
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
        step: PipelineBatchTaskStep,
        totalCount: Int,
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
                STATUS,
                STEP,
                TOTAL_COUNT,
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
                PipelineBatchTaskStatus.DRAFT.name,
                step.name,
                totalCount,
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
    ): PipelineBatchTaskInfo? {
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
    ): List<PipelineBatchTaskInfo> {
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
                query.set(TASK_PARAM, update.taskParam)
            }
            if (update.status != null) {
                query.set(STATUS, update.status.name)
            }
            if (update.successCount != null) {
                query.set(SUCCESS_COUNT, update.successCount)
            }
            if (update.failedCount != null) {
                query.set(FAILED_COUNT, update.failedCount)
            }
            if (update.step != null) {
                query.set(STEP, update.step.name)
            }
            query.set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
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

    private fun convert(record: Record): PipelineBatchTaskInfo {
        return with(T_PIPELINE_BATCH_TASK) {
            PipelineBatchTaskInfo(
                taskId = record.get(TASK_ID),
                projectId = record.get(PROJECT_ID),
                taskName = record.get(TASK_NAME),
                taskType = PipelineBatchTaskType.valueOf(record.get(TASK_TYPE)),
                taskParam = record.get(TASK_PARAM),
                status = PipelineBatchTaskStatus.valueOf(record.get(STATUS)),
                step = PipelineBatchTaskStep.valueOf(record.get(STEP)),
                totalCount = record.get(TOTAL_COUNT),
                successCount = record.get(SUCCESS_COUNT),
                failedCount = record.get(FAILED_COUNT),
                creator = record.get(CREATOR),
                createTime = record.get(CREATE_TIME),
                updateTime = record.get(UPDATE_TIME)
            )
        }
    }
}
