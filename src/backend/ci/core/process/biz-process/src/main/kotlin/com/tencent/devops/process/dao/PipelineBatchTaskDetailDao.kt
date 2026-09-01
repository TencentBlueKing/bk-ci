package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.api.util.toLocalDateTime
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.model.process.Tables.T_PIPELINE_BATCH_TASK_DETAIL
import com.tencent.devops.model.process.tables.records.TPipelineBatchTaskDetailRecord
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailErrorType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatusSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailUpdate
import com.tencent.devops.process.service.task.copy.PipelineCopyTaskUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBatchTaskDetailDao {

    fun batchCreate(
        dslContext: DSLContext,
        details: List<PipelineBatchTaskDetail>
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
                    PIPELINE_CREATOR,
                    PAC,
                    CONSTRAINT,
                    SUB_PIPELINE,
                    LOCKED,
                    VERSION_STATUS,
                    CHANGE,
                    STATUS,
                    ERROR_MESSAGE,
                    ERROR_TYPE,
                    START_TIME,
                    END_TIME
                ).values(
                    detail.taskId,
                    detail.projectId,
                    detail.taskType.name,
                    detail.pipelineId,
                    detail.pipelineName,
                    detail.pipelineCreator,
                    detail.pac,
                    detail.constraint,
                    detail.subPipeline,
                    detail.locked,
                    detail.versionStatus?.name,
                    detail.change,
                    detail.status.name,
                    PipelineCopyTaskUtils.toErrorMessageJson(detail.errorMessage),
                    detail.errorType?.name,
                    detail.startTime?.toLocalDateTime(),
                    detail.endTime?.toLocalDateTime()
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
        pipelineCreator: String? = null,
        status: PipelineBatchTaskDetailStatus? = null,
        pac: Boolean? = null,
        subPipeline: Boolean? = null,
        change: Boolean? = null
    ): Long {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectCount()
                .from(this)
                .where(
                    buildTaskConditions(
                        projectId = projectId,
                        taskId = taskId,
                        pipelineName = pipelineName,
                        pipelineCreator = pipelineCreator,
                        status = status,
                        pac = pac,
                        subPipeline = subPipeline,
                        change = change
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
        pipelineCreator: String? = null,
        status: PipelineBatchTaskDetailStatus? = null,
        pac: Boolean? = null,
        subPipeline: Boolean? = null,
        change: Boolean? = null,
        pipelineIds: Set<String>? = null,
        resourceIds: Set<String>? = null,
        offset: Int? = null,
        limit: Int? = null
    ): List<PipelineBatchTaskDetail> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            val query = dslContext.selectFrom(this)
                .where(
                    buildTaskConditions(
                        projectId = projectId,
                        taskId = taskId,
                        pipelineName = pipelineName,
                        pipelineCreator = pipelineCreator,
                        status = status,
                        pac = pac,
                        subPipeline = subPipeline,
                        change = change,
                        pipelineIds = pipelineIds,
                        resourceIds = resourceIds
                    )
                )
                .orderBy(PIPELINE_NAME.asc(), PIPELINE_ID.asc())
            if (offset != null && limit != null) {
                query.limit(offset, limit).fetch().map(::convert)
            } else {
                query.fetch().map(::convert)
            }
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
    ): PipelineBatchTaskDetail? {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchOne()
                ?.let(::convert)
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>,
        status: PipelineBatchTaskDetailStatus,
        change: Boolean
    ): Int {
        if (pipelineIds.isEmpty()) {
            return 0
        }
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            val query = dslContext.update(this)
                .set(STATUS, status.name)
                .set(CHANGE, change)
            query.where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        update: PipelineBatchTaskDetailUpdate
    ): Int {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            val query = dslContext.update(this)
            update.status?.let { query.set(STATUS, it.name) }
            update.change?.let { query.set(CHANGE, it) }
            update.errorType?.let { query.set(ERROR_TYPE, it.name) }
            update.errorMessage?.let {
                query.set(ERROR_MESSAGE, PipelineCopyTaskUtils.toErrorMessageJson(it))
            }
            if (update.clearErrorMessage) {
                query.setNull(ERROR_MESSAGE)
            }
            if (update.clearErrorType) {
                query.setNull(ERROR_TYPE)
            }
            query.set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
                .and(PIPELINE_ID.eq(update.pipelineId))
                .execute()
        }
    }

    fun batchUpdate(
        dslContext: DSLContext,
        updates: List<PipelineBatchTaskDetailUpdate>
    ): Int {
        if (updates.isEmpty()) {
            return 0
        }
        val queries = with(T_PIPELINE_BATCH_TASK_DETAIL) {
            updates.map { update ->
                val query = dslContext.update(this)
                update.status?.let { query.set(STATUS, it.name) }
                update.change?.let { query.set(CHANGE, it) }
                update.errorType?.let { query.set(ERROR_TYPE, it.name) }
                update.errorMessage?.let {
                    query.set(ERROR_MESSAGE, PipelineCopyTaskUtils.toErrorMessageJson(it))
                }
                if (update.clearErrorMessage) {
                    query.setNull(ERROR_MESSAGE)
                }
                if (update.clearErrorType) {
                    query.setNull(ERROR_TYPE)
                }
                query.set(UPDATE_TIME, LocalDateTime.now())
                    .where(PROJECT_ID.eq(update.projectId))
                    .and(TASK_ID.eq(update.taskId))
                    .and(PIPELINE_ID.eq(update.pipelineId))
            }
        }
        return dslContext.batch(queries).execute().sum()
    }

    fun updateChange(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        change: Boolean
    ): Int {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.update(this)
                .set(CHANGE, change)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .execute()
        }
    }

    fun deleteByPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>
    ): Int {
        if (pipelineIds.isEmpty()) {
            return 0
        }
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    private fun buildTaskConditions(
        projectId: String,
        taskId: String,
        pipelineName: String? = null,
        pipelineCreator: String? = null,
        status: PipelineBatchTaskDetailStatus? = null,
        pac: Boolean? = null,
        subPipeline: Boolean? = null,
        change: Boolean? = null,
        pipelineIds: Set<String>? = null,
        resourceIds: Set<String>? = null
    ): List<Condition> {
        return with(T_PIPELINE_BATCH_TASK_DETAIL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(TASK_ID.eq(taskId))
            if (!pipelineName.isNullOrBlank()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            if (!pipelineCreator.isNullOrBlank()) {
                conditions.add(PIPELINE_CREATOR.like("%$pipelineCreator%"))
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
            if (change != null) {
                conditions.add(CHANGE.eq(change))
            }
            if (!pipelineIds.isNullOrEmpty()) {
                conditions.add(PIPELINE_ID.`in`(pipelineIds))
            }
            if (!resourceIds.isNullOrEmpty()) {
                conditions.add(PIPELINE_ID.`in`(resourceIds))
            }
            conditions
        }
    }

    private fun convert(record: TPipelineBatchTaskDetailRecord): PipelineBatchTaskDetail {
        return with(record) {
            PipelineBatchTaskDetail(
                taskId = taskId,
                projectId = projectId,
                taskType = PipelineBatchTaskType.valueOf(taskType),
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                pipelineCreator = pipelineCreator,
                pac = pac,
                constraint = constraint,
                subPipeline = subPipeline,
                locked = locked,
                versionStatus = versionStatus?.let { VersionStatus.valueOf(it) },
                change = change,
                status = PipelineBatchTaskDetailStatus.valueOf(status),
                errorType = errorType?.let { PipelineBatchTaskDetailErrorType.valueOf(it) },
                errorMessage = PipelineCopyTaskUtils.parseErrorMessage(errorMessage),
                startTime = startTime?.timestampmilli(),
                endTime = endTime?.timestampmilli(),
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}
