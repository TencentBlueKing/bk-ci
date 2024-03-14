package com.tencent.devops.process.service.builds

import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.PipelineBuildCommit
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildCommitService {

    fun create(
        projectId: String,
        pipelineId: String,
        buildId: String,
        matcher: ScmWebhookMatcher,
        repo: Repository
    ) {
        logger.info("start create pipeline build commits|$projectId|$pipelineId|$buildId|$repo")
    }

    fun saveCommits(commits: List<PipelineBuildCommit>) {
        logger.info("start create pipeline build commits|$commits")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildCommitService::class.java)
    }
}
