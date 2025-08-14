package com.tencent.devops.process.plugin.check.dao

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
                REPO_HASH_ID,
                CONTEXT,
                REF,
                EXT_REF,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                BUILD_NUM,
                BUILD_STATUS,
                CHECK_RUN_ID,
                CHECK_RUN_STATUS,
                REPO_SCM_CODE,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                buildCheckRun.repoHashId,
                buildCheckRun.context,
                buildCheckRun.ref,
                buildCheckRun.extRef,
                buildCheckRun.projectId,
                buildCheckRun.pipelineId,
                buildCheckRun.buildId,
                buildCheckRun.buildNum,
                buildCheckRun.buildStatus.name,
                buildCheckRun.checkRunId,
                buildCheckRun.checkRunStatus?.name,
                buildCheckRun.scmCode,
                now,
                now
            ).onDuplicateKeyUpdate()
                    .set(BUILD_NUM, buildCheckRun.buildNum)
                    .set(UPDATE_TIME, now)
                    .set(CHECK_RUN_STATUS, buildCheckRun.checkRunStatus?.name)
                    .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        repoHashId: String,
        context: String,
        ref: String,
        extRef: String
    ) = with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
        val conditions = listOf(
            REPO_HASH_ID.eq(repoHashId),
            CONTEXT.eq(context),
            REF.eq(ref),
            EXT_REF.eq(extRef)
        )
        dslContext.selectFrom(this)
                .where(conditions)
                .fetchAny()
    }

    fun update(
        dslContext: DSLContext,
        record: TPipelineBuildCheckRunRecord
    ) {
        with(TPipelineBuildCheckRun.T_PIPELINE_BUILD_CHECK_RUN) {
            val conditions = listOf(
                REPO_HASH_ID.eq(record.repoHashId),
                CONTEXT.eq(record.context),
                REF.eq(record.ref),
                EXT_REF.eq(record.extRef)
            )
            dslContext.update(this)
                    .set(record)
                    .where(conditions)
                    .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        repoHashId: String,
        context: String,
        ref: String,
        extRef: String,
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
                        it.set(REPO_HASH_ID, repoHashId)
                    }
                    .where(
                        listOf(
                            REPO_HASH_ID.eq(repoHashId),
                            CONTEXT.eq(context),
                            REF.eq(ref),
                            EXT_REF.eq(extRef)
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
}