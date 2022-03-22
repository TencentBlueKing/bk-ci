package com.tencent.devops.process.service.builds

import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.dao.PipelineBuildCommitsDao
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.GitCommit
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildCommitsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBuildCommits: PipelineBuildCommitsDao
) {

    fun create(
        projectId: String,
        pipelineId: String,
        buildId: String,
        matcher: ScmWebhookMatcher,
        repo: Repository
    ) {
        val gitCommitList = matcher.getWebhookCommitList(
            projectId = projectId,
            pipelineId = pipelineId,
            repository = repo
        )
        pipelineBuildCommits.create(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            webhookCommits = gitCommitList,
            mrId = matcher.getMergeRequestId()?.toString() ?: ""
        )
    }
}