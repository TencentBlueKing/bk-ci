package com.tencent.devops.process.plugin.trigger.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_TIMER_BRANCH
import com.tencent.devops.model.process.tables.TPipelineTimerBranch
import com.tencent.devops.model.process.tables.records.TPipelineTimerBranchRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTimerBranchDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        taskId: String,
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
                TASK_ID,
                REPO_HASH_ID,
                BRANCH,
                REVISION,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                pipelineId,
                taskId,
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
        taskId: String,
        repoHashId: String,
        branch: String
    ): TPipelineTimerBranchRecord? {
        return with(T_PIPELINE_TIMER_BRANCH) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(TASK_ID.eq(taskId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(BRANCH.eq(branch))
                .fetchAny()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Result<TPipelineTimerBranchRecord> {
        return with(T_PIPELINE_TIMER_BRANCH) {
            dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String?,
        pipelineId: String?,
        limit: Int?,
        offset: Int?
    ): List<Pair<String, String>> {
        return with(T_PIPELINE_TIMER_BRANCH) {
            val conditions = mutableListOf<Condition>()
            if (!projectId.isNullOrBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            dslContext.select(PROJECT_ID, PIPELINE_ID)
                    .from(this)
                    .where(conditions)
                    .groupBy(PROJECT_ID, PIPELINE_ID)
                    .orderBy(PROJECT_ID, PIPELINE_ID)
                    .let {
                        if (limit != null && offset != null) {
                            it.limit(limit).offset(offset)
                        }
                        it
                    }
                    .fetch()
                    .map { it.getValue(PROJECT_ID) to it.getValue(PIPELINE_ID) }
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        taskId: String?,
        repoHashId: String?,
        branch: String?
    ): Int {
        with(T_PIPELINE_TIMER_BRANCH) {
            val conditions = buildConditions(projectId, pipelineId, taskId, repoHashId, branch)
            return dslContext.deleteFrom(this)
                    .where(conditions)
                    .execute()
        }
    }

    private fun TPipelineTimerBranch.buildConditions(
        projectId: String,
        pipelineId: String,
        taskId: String?,
        repoHashId: String?,
        branch: String?
    ): List<Condition> {
        val conditions = mutableListOf(
            PROJECT_ID.eq(projectId),
            PIPELINE_ID.eq(pipelineId)
        )
        if (taskId != null) {
            conditions.add(TASK_ID.eq(taskId))
        }
        if (repoHashId != null) {
            conditions.add(REPO_HASH_ID.eq(repoHashId))
        }
        if (branch != null) {
            conditions.add(BRANCH.eq(branch))
        }
        return conditions
    }

    fun deleteEmptyTaskId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        with(T_PIPELINE_TIMER_BRANCH) {
            return dslContext.deleteFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(TASK_ID.isNull().or(TASK_ID.eq("")))
                    .execute()
        }
    }

    fun updateTimerBranch(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        sourceRepoHashId: String?,
        sourceBranch: String?,
        sourceTaskId: String?,
        targetTaskId: String
    ): Int {
        with(T_PIPELINE_TIMER_BRANCH) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                PIPELINE_ID.eq(pipelineId)
            )
            if (sourceTaskId != null) {
                conditions.add(TASK_ID.eq(sourceTaskId))
            }
            if (sourceRepoHashId != null) {
                conditions.add(REPO_HASH_ID.eq(sourceRepoHashId))
            }
            if (sourceBranch != null) {
                conditions.add(BRANCH.eq(sourceBranch))
            }
            return dslContext.update(this)
                    .set(TASK_ID, targetTaskId)
                    .where(conditions)
                    .execute()
        }
    }
}
