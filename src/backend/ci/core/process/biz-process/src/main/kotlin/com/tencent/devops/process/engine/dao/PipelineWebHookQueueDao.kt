package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.Tables.T_PIPELINE_WEBHOOK_QUEUE
import com.tencent.devops.model.process.tables.records.TPipelineWebhookQueueRecord
import com.tencent.devops.process.engine.pojo.PipelineWebHookQueue
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineWebHookQueueDao {

    fun save(
        dslContext: DSLContext,
        pipelineId: String,
        sourceProjectId: Long,
        sourceRepoName: String,
        sourceBranch: String,
        targetProjectId: Long,
        targetRepoName: String,
        targetBranch: String,
        buildId: String
    ) {
        with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                SOURCE_PROJECT_ID,
                SOURCE_REPO_NAME,
                SOURCE_BRANCH,
                TARGET_PROJECT_ID,
                TARGET_REPO_NAME,
                TARGET_BRANCH,
                BUILD_ID,
                CREATE_TIME
            ).values(
                pipelineId,
                sourceProjectId,
                sourceRepoName,
                sourceBranch,
                targetProjectId,
                targetRepoName,
                targetBranch,
                buildId,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getWebHookBuildHistory(
        dslContext: DSLContext,
        pipelineId: String,
        sourceProjectId: Long,
        sourceBranch: String,
        targetProjectId: Long,
        targetBranch: String
    ): List<PipelineWebHookQueue>? {
        return with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(SOURCE_PROJECT_ID.eq(sourceProjectId))
                .and(SOURCE_BRANCH.eq(sourceBranch))
                .and(TARGET_PROJECT_ID.eq(targetProjectId))
                .and(TARGET_BRANCH.eq(targetBranch))
                .fetch()
        }.map {
            convert(it)
        }
    }

    fun get(
        dslContext: DSLContext,
        buildId: String
    ): PipelineWebHookQueue? {
        return with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne()
        }?.let { convert(it) }
    }

    fun deleteByBuildIds(
        dslContext: DSLContext,
        buildIds: List<String>
    ) {
        with(T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }

    fun convert(record: TPipelineWebhookQueueRecord): PipelineWebHookQueue {
        return with(record) {
            PipelineWebHookQueue(
                id = id,
                pipelineId = pipelineId,
                sourceProjectId = sourceProjectId,
                sourceRepoName = sourceRepoName,
                sourceBranch = sourceBranch,
                targetProjectId = targetProjectId,
                targetRepoName = targetRepoName,
                targetBranch = targetBranch,
                buildId = buildId,
                createTime = createTime.timestampmilli()
            )
        }
    }
}
