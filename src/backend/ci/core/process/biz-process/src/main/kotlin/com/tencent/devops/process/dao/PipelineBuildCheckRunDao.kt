package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineBuildCheckRun
import com.tencent.devops.model.process.tables.records.TPipelineBuildCheckRunRecord
import com.tencent.devops.process.trigger.pojo.PipelineBuildCheckRun
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineBuildCheckRunDao {
    fun create(
        dslContext: DSLContext,
        buildCheckRun: PipelineBuildCheckRun
    ) {
        val now = LocalDateTime.now()
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                BUILD_NUM,
                BUILD_STATUS,
                REPO_HASH_ID,
                CONTEXT,
                COMMIT_ID,
                PULL_REQUEST_ID,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                buildCheckRun.projectId,
                buildCheckRun.pipelineId,
                buildCheckRun.buildId,
                buildCheckRun.buildNum,
                buildCheckRun.buildStatus.name,
                buildCheckRun.repoHashId,
                buildCheckRun.context,
                buildCheckRun.commitId,
                buildCheckRun.pullRequestId,
                now,
                now
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildStatus: String? = null,
        checkRunId: Long? = null,
        checkRunStatus: String? = null
    ) {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            val update = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
            if (checkRunId != null) {
                update.set(CHECK_RUN_ID, checkRunId)
            }
            if (!checkRunStatus.isNullOrBlank()) {
                update.set(CHECK_RUN_STATUS, checkRunStatus)
            }
            if (!buildStatus.isNullOrBlank()) {
                update.set(BUILD_STATUS, buildStatus)
            }
            update.where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun getLatestCheckRun(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        commitId: String,
        pullRequestId: Long
    ): TPipelineBuildCheckRunRecord? {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                PIPELINE_ID.eq(pipelineId),
                REPO_HASH_ID.eq(repoHashId),
                COMMIT_ID.eq(commitId),
                PULL_REQUEST_ID.eq(pullRequestId),
                CHECK_RUN_ID.isNotNull
            )
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(BUILD_NUM.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): TPipelineBuildCheckRunRecord? {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .fetchOne()
        }
    }
}
