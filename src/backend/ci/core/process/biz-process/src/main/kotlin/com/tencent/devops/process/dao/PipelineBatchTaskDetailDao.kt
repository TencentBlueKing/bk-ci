package com.tencent.devops.process.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_BATCH_TASK_DETAIL
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatusSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

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
                    TASK_TYPE,
                    PIPELINE_ID,
                    PIPELINE_NAME,
                    PAC,
                    CONSTRAINT,
                    SUB_PIPELINE,
                    LOCKED,
                    CHANGE,
                    STATUS,
                    ERROR_MESSAGE,
                    START_TIME,
                    END_TIME
                ).values(
                    detail.taskId,
                    detail.projectId,
                    detail.taskType.name,
                    detail.pipelineId,
                    detail.pipelineName,
                    detail.pac,
                    detail.constraint,
                    detail.subPipeline,
                    detail.locked,
                    detail.change,
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
        taskId: String,
        pipelineName: String? = null,
        status: PipelineBatchTaskDetailStatus? = null,
        pac: Boolean? = null,
        subPipeline: Boolean? = null
    ): Long {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectCount()
                .from(this)
                .where(
                    buildTaskConditions(
                        projectId = projectId,
                        taskId = taskId,
                        pipelineName = pipelineName,
                        status = status,
                        pac = pac,
                        subPipeline = subPipeline
                    )
                )
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineName: String? = null,
        status: PipelineBatchTaskDetailStatus? = null,
        pac: Boolean? = null,
        subPipeline: Boolean? = null,
        offset: Int,
        limit: Int
    ): List<PipelineBatchTaskDetailInfo> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectFrom(this)
                .where(
                    buildTaskConditions(
                        projectId = projectId,
                        taskId = taskId,
                        pipelineName = pipelineName,
                        status = status,
                        pac = pac,
                        subPipeline = subPipeline
                    )
                )
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

    fun detailStatusSummary(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        taskType: PipelineBatchTaskType
    ): List<PipelineBatchTaskDetailStatusSummary> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.select(STATUS, DSL.count())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(TASK_TYPE.eq(taskType.name))
                .groupBy(STATUS)
                .fetch()
                .map { record ->
                    PipelineBatchTaskDetailStatusSummary(
                        status = PipelineBatchTaskDetailStatus.valueOf(record.get(STATUS)),
                        count = record.value2().toLong()
                    )
                }
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): PipelineBatchTaskDetailInfo? {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchOne()
                ?.let(::convert)
        }
    }

    fun update(
        dslContext: DSLContext,
        update: PipelineBatchTaskDetailUpdate
    ): Int {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            val query = dslContext.update(this)
            if (update.status != null) {
                query.set(STATUS, update.status.name)
            }
            if (update.change != null) {
                query.set(CHANGE, update.change)
            }
            query.where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
                .and(PIPELINE_ID.eq(update.pipelineId))
                .execute()
        }
    }

    private fun buildTaskConditions(
        projectId: String,
        taskId: String,
        pipelineName: String? = null,
        status: PipelineBatchTaskDetailStatus? = null,
        pac: Boolean? = null,
        subPipeline: Boolean? = null
    ): List<Condition> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(TASK_ID.eq(taskId))
            if (!pipelineName.isNullOrBlank()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            if (pac != null) {
                conditions.add(PAC.eq(pac))
            }
            if (subPipeline != null) {
                conditions.add(SUB_PIPELINE.eq(subPipeline))
            }
            conditions
        }
    }

    private fun convert(record: Record): PipelineBatchTaskDetailInfo {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            PipelineBatchTaskDetailInfo(
                taskId = record.get(TASK_ID),
                projectId = record.get(PROJECT_ID),
                taskType = PipelineBatchTaskType.valueOf(record.get(TASK_TYPE)),
                pipelineId = record.get(PIPELINE_ID),
                pipelineName = record.get(PIPELINE_NAME),
                pac = record.get(PAC),
                constraint = record.get(CONSTRAINT),
                subPipeline = record.get(SUB_PIPELINE),
                locked = record.get(LOCKED),
                change = record.get(CHANGE),
                status = PipelineBatchTaskDetailStatus.valueOf(record.get(STATUS)),
                errorMessage = record.get(ERROR_MESSAGE),
                startTime = record.get(START_TIME),
                endTime = record.get(END_TIME),
                createTime = record.get(CREATE_TIME),
                updateTime = record.get(UPDATE_TIME)
            )
        }
    }
}
