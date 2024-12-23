package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.repository.dao.GitCheckDao
import com.tencent.devops.repository.pojo.ExecuteSource
import com.tencent.devops.repository.pojo.RepositoryGitCheck
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCheckService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitCheckDao: GitCheckDao
) {
    fun getGitCheck(
        pipelineId: String,
        repositoryConfig: RepositoryConfig,
        commitId: String,
        targetBranch: String?,
        context: String
    ): RepositoryGitCheck? {
        val result = gitCheckDao.getOrNull(
            dslContext, pipelineId, repositoryConfig, commitId, context, targetBranch
        ) ?: return null
        return RepositoryGitCheck(
            gitCheckId = result.id,
            pipelineId = result.pipelineId,
            buildNumber = result.buildNumber,
            repositoryId = result.repoId,
            repositoryName = result.repoName,
            commitId = result.commitId,
            context = result.context,
            source = ExecuteSource.valueOf(result.source),
            targetBranch = result.targetBranch
        )
    }

    fun creatGitCheck(
        repositoryGitCheck: RepositoryGitCheck
    ) {
        gitCheckDao.create(dslContext, repositoryGitCheck)
    }

    fun updateGitCheck(
        gitCheckId: Long,
        buildNumber: Int
    ) {
        gitCheckDao.update(
            dslContext = dslContext,
            id = gitCheckId,
            buildNumber = buildNumber
        )
    }
}
