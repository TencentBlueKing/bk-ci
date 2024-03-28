package com.tencent.devops.process.plugin.trigger.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_TIMER_BRANCH
import com.tencent.devops.model.process.tables.records.TPipelineTimerBranchRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTimerBranchDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        branch: String,
        revision: String
    ): Int {
        val now = LocalDateTime.now()
        return with(T_PIPELINE_TIMER_BRANCH) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                REPO_HASH_ID,
                BRANCH,
                REVISION,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                pipelineId,
                repoHashId,
                branch,
                revision,
                now,
                now
            ).onDuplicateKeyUpdate()
                .set(REVISION, revision)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        branch: String
    ): TPipelineTimerBranchRecord? {
        return with(T_PIPELINE_TIMER_BRANCH) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(BRANCH.eq(branch))
                .fetchAny()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) {
        with(T_PIPELINE_TIMER_BRANCH) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}
