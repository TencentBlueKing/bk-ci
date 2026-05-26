package com.tencent.devops.process.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_COPY_TASK
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskUpdate
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class PipelineCopyTaskDao {

    fun create(
        dslContext: DSLContext,
        pipelineCopyTaskInfo: PipelineCopyTaskInfo
    ): Int {
        return with(T_PIPELINE_COPY_TASK) {
            dslContext.insertInto(
                this,
                TASK_ID,
                PROJECT_ID,
                TARGET_PROJECT_ID,
                PIPELINE_COPY_STRATEGY,
                STATUS,
                PIPELINE_COUNT,
                SUB_PIPELINE_COUNT,
                PAC_COUNT,
                UNPROCESSED_COUNT,
                HIGH_RISK_COUNT,
                AUTO_FINISH_COUNT
            ).values(
                pipelineCopyTaskInfo.taskId,
                pipelineCopyTaskInfo.projectId,
                pipelineCopyTaskInfo.targetProjectId,
                pipelineCopyTaskInfo.pipelineCopyStrategy.name,
                pipelineCopyTaskInfo.status.name,
                pipelineCopyTaskInfo.pipelineCount,
                pipelineCopyTaskInfo.subPipelineCount,
                pipelineCopyTaskInfo.pacCount,
                pipelineCopyTaskInfo.unprocessedCount,
                pipelineCopyTaskInfo.highRiskCount,
                pipelineCopyTaskInfo.autoFinishCount
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        taskId: String
    ): PipelineCopyTaskInfo? {
        return with(T_PIPELINE_COPY_TASK) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TASK_ID.eq(taskId))
                .fetchOne()
                ?.let(::convert)
        }
    }

    fun update(
        dslContext: DSLContext,
        update: PipelineCopyTaskUpdate
    ): Int {
        return with(T_PIPELINE_COPY_TASK) {
            val query = dslContext.update(this)
            update.targetProjectId?.let { query.set(TARGET_PROJECT_ID, it) }
            update.pipelineCopyStrategy?.let { query.set(PIPELINE_COPY_STRATEGY, it.name) }
            update.status?.let { query.set(STATUS, it.name) }
            update.pipelineCount?.let { query.set(PIPELINE_COUNT, it) }
            update.subPipelineCount?.let { query.set(SUB_PIPELINE_COUNT, it) }
            update.pacCount?.let { query.set(PAC_COUNT, it) }
            update.unprocessedCount?.let { query.set(UNPROCESSED_COUNT, it) }
            update.highRiskCount?.let { query.set(HIGH_RISK_COUNT, it) }
            update.autoFinishCount?.let { query.set(AUTO_FINISH_COUNT, it) }
            query.where(PROJECT_ID.eq(update.projectId))
                .and(TASK_ID.eq(update.taskId))
                .execute()
        }
    }

    private fun convert(record: Record): PipelineCopyTaskInfo {
        return with(T_PIPELINE_COPY_TASK) {
            PipelineCopyTaskInfo(
                taskId = record.get(TASK_ID),
                projectId = record.get(PROJECT_ID),
                targetProjectId = record.get(TARGET_PROJECT_ID),
                pipelineCopyStrategy = PipelineCopyStrategy.valueOf(record.get(PIPELINE_COPY_STRATEGY)),
                status = PipelineBatchTaskStatus.valueOf(record.get(STATUS)),
                pipelineCount = record.get(PIPELINE_COUNT),
                subPipelineCount = record.get(SUB_PIPELINE_COUNT),
                pacCount = record.get(PAC_COUNT),
                unprocessedCount = record.get(UNPROCESSED_COUNT),
                highRiskCount = record.get(HIGH_RISK_COUNT),
                autoFinishCount = record.get(AUTO_FINISH_COUNT),
                createTime = record.get(CREATE_TIME),
                updateTime = record.get(UPDATE_TIME)
            )
        }
    }
}
