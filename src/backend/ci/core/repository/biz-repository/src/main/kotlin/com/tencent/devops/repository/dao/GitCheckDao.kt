package com.tencent.devops.repository.dao

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.model.repository.tables.TRepositoryGitCheck
import com.tencent.devops.model.repository.tables.records.TRepositoryGitCheckRecord
import com.tencent.devops.repository.pojo.RepositoryGitCheck
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitCheckDao {

    fun getOrNull(
        dslContext: DSLContext,
        pipelineId: String,
        repositoryConfig: RepositoryConfig,
        commitId: String,
        context: String,
        targetBranch: String?
    ): TRepositoryGitCheckRecord? {
        with(TRepositoryGitCheck.T_REPOSITORY_GIT_CHECK) {
            val step = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(COMMIT_ID.eq(commitId))
                .and(CONTEXT.eq(context))
                .and(TARGET_BRANCH.eq(targetBranch ?: ""))
            when (repositoryConfig.repositoryType) {
                RepositoryType.ID -> step.and(REPO_ID.eq(repositoryConfig.getRepositoryId()))
                RepositoryType.NAME -> step.and(REPO_NAME.eq(repositoryConfig.getRepositoryId()))
            }
            return step.fetchAny()
        }
    }

    fun create(
        dslContext: DSLContext,
        repositoryGitCheck: RepositoryGitCheck
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryGitCheck.T_REPOSITORY_GIT_CHECK) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                BUILD_NUMBER,
                REPO_ID,
                REPO_NAME,
                COMMIT_ID,
                CREATE_TIME,
                UPDATE_TIME,
                CONTEXT,
                SOURCE,
                TARGET_BRANCH,
                CHECK_RUN_ID
            ).values(
                repositoryGitCheck.pipelineId,
                repositoryGitCheck.buildNumber,
                repositoryGitCheck.repositoryId,
                repositoryGitCheck.repositoryName,
                repositoryGitCheck.commitId,
                now,
                now,
                repositoryGitCheck.context,
                repositoryGitCheck.source.name,
                repositoryGitCheck.targetBranch,
                repositoryGitCheck.checkRunId
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        buildNumber: Int,
        checkRunId: Long?
    ) {
        with(TRepositoryGitCheck.T_REPOSITORY_GIT_CHECK) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(BUILD_NUMBER, buildNumber)
                .let {
                    if (checkRunId != null) {
                        it.set(CHECK_RUN_ID, checkRunId)
                    }
                    it
                }
                .where(ID.eq(id))
                .execute()
        }
    }
}
