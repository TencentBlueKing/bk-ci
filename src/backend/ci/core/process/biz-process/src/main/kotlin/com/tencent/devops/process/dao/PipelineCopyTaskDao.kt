package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_COPY_TASK
import com.tencent.devops.model.process.tables.records.TPipelineCopyTaskRecord
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskInfo
import org.jooq.DSLContext
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
                TASK_NAME,
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
                pipelineCopyTaskInfo.taskName,
                pipelineCopyTaskInfo.targetProjectId,
                pipelineCopyTaskInfo.pipelineCopyStrategy?.name,
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

    private fun convert(record: TPipelineCopyTaskRecord): PipelineCopyTaskInfo {
        return with(record) {
            PipelineCopyTaskInfo(
                taskId = taskId,
                projectId = projectId,
                taskName = taskName,
                targetProjectId = targetProjectId,
                pipelineCopyStrategy = pipelineCopyStrategy?.let { PipelineCopyStrategy.valueOf(it) },
                status = PipelineBatchTaskStatus.valueOf(status),
                pipelineCount = pipelineCount,
                subPipelineCount = subPipelineCount,
                pacCount = pacCount,
                unprocessedCount = unprocessedCount,
                highRiskCount = highRiskCount,
                autoFinishCount = autoFinishCount,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}
