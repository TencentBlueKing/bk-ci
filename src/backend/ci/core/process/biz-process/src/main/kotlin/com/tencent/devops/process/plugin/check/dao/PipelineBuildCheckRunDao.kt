package com.tencent.devops.process.plugin.check.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.tables.TPipelineBuildCheckRun
import com.tencent.devops.model.process.tables.records.TPipelineBuildCheckRunRecord
import com.tencent.devops.process.pojo.PipelineBuildCheckRun
import org.jooq.DSLContext
import org.jooq.Result
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
                CHECK_RUN_ID,
                CHECK_RUN_STATUS,
                REPO_SCM_CODE,
                EXTENSION_DATA,
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
                buildCheckRun.pullRequestBizId,
                buildCheckRun.checkRunId,
                buildCheckRun.checkRunStatus?.name,
                buildCheckRun.scmCode,
                buildCheckRun.extensionData?.let { JsonUtil.toJson(it, false) },
                now,
                now
            ).onDuplicateKeyUpdate()
                    .set(BUILD_NUM, buildCheckRun.buildNum)
                    .set(UPDATE_TIME, now)
                    .set(CHECK_RUN_STATUS, buildCheckRun.checkRunStatus?.name)
                    .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        record: TPipelineBuildCheckRunRecord
    ) {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            val conditions = listOf(
                REPO_HASH_ID.eq(record.repoHashId),
                CONTEXT.eq(record.context),
                COMMIT_ID.eq(record.commitId),
                PULL_REQUEST_ID.eq(record.pullRequestId)
            )
            dslContext.update(this)
                    .set(record)
                    .where(conditions)
                    .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        checkRunId: Long?,
        checkRunStatus: String?
    ) {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            dslContext.update(this)
                    .let {
                        if (checkRunId != null) {
                            it.set(CHECK_RUN_ID, checkRunId)
                        }
                        if (!checkRunStatus.isNullOrBlank()) {
                            it.set(CHECK_RUN_STATUS, checkRunStatus)
                        }
                        it.set(BUILD_ID, buildId)
                    }
                    .where(
                        listOf(
                            PROJECT_ID.eq(projectId),
                            PIPELINE_ID.eq(pipelineId),
                            BUILD_ID.eq(buildId)
                        )
                    )
                    .execute()
        }
    }

    fun getCheckRun(
        dslContext: DSLContext,
        checkRunStatus: String,
        buildStatus: Set<String>
    ): Result<TPipelineBuildCheckRunRecord> {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            val conditions = listOf(
                CHECK_RUN_STATUS.eq(checkRunStatus),
                BUILD_STATUS.`in`(buildStatus)
            )
            return dslContext.selectFrom(this)
                    .where(conditions)
                    .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        commitId: String,
        pullRequestId: String
    ) : Result<TPipelineBuildCheckRunRecord> {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            val conditions = listOf(
                PROJECT_ID.eq(projectId),
                PIPELINE_ID.eq(pipelineId),
                REPO_HASH_ID.eq(repoHashId),
                COMMIT_ID.eq(commitId),
                PULL_REQUEST_ID.eq(pullRequestId)
            )
            return dslContext.selectFrom(this)
                    .where(conditions)
                    .orderBy(BUILD_NUM.desc())
                    .fetch()
        }
    }
}