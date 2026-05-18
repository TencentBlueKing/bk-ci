package com.tencent.devops.process.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_BATCH_TASK
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
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
        taskName: String,
        taskType: PipelineBatchTaskType,
        taskParam: String?,
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

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        status: PipelineBatchTaskStatus,
        successCount: Int? = null,
        failedCount: Int? = null
    ): Int {
        return with(T_PIPELINE_BATCH_TASK) {
            val update = dslContext.update(this)
                .set(STATUS, status.name)
                .set(UPDATE_TIME, LocalDateTime.now())
            if (successCount != null) {
                update.set(SUCCESS_COUNT, successCount)
            }
            if (failedCount != null) {
                update.set(FAILED_COUNT, failedCount)
            }
            update.where(PROJECT_ID.eq(projectId))
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
