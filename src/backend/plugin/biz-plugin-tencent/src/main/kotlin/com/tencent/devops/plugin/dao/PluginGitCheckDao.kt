package com.tencent.devops.plugin.dao

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.model.plugin.tables.TPluginGitCheck
import com.tencent.devops.model.plugin.tables.records.TPluginGitCheckRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PluginGitCheckDao {

    fun getOrNull(
        dslContext: DSLContext,
        pipelineId: String,
        repositoryConfig: RepositoryConfig,
        commitId: String
    ): TPluginGitCheckRecord? {
        with(TPluginGitCheck.T_PLUGIN_GIT_CHECK) {
            val step = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(COMMIT_ID.eq(commitId))
            when (repositoryConfig.repositoryType) {
                RepositoryType.ID -> step.and(REPO_ID.eq(repositoryConfig.getRepositoryId()))
                RepositoryType.NAME -> step.and(REPO_NAME.eq(repositoryConfig.getRepositoryId()))
            }
            return step.fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        buildNumber: Int,
        repositoryConfig: RepositoryConfig,
        commitId: String
    ) {
        val now = LocalDateTime.now()
        with(TPluginGitCheck.T_PLUGIN_GIT_CHECK) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                BUILD_NUMBER,
                REPO_ID,
                REPO_NAME,
                COMMIT_ID,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                pipelineId,
                buildNumber,
                repositoryConfig.repositoryHashId,
                repositoryConfig.repositoryName,
                commitId,
                now,
                now
            ).execute()
        }
    }

    fun update(dslContext: DSLContext, id: Long, buildNumber: Int) {
        with(TPluginGitCheck.T_PLUGIN_GIT_CHECK) {
            dslContext.update(this)
                .set(BUILD_NUMBER, buildNumber)
                .where(ID.eq(id))
                .execute()
        }
    }
}