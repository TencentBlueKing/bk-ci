package com.tencent.devops.process.service.builds

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCommitsFinishEvent
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.dao.PipelineBuildCommitsDao
import com.tencent.devops.repository.pojo.Repository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildCommitsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBuildCommitsDao: PipelineBuildCommitsDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {

    fun create(
        projectId: String,
        pipelineId: String,
        buildId: String,
        matcher: ScmWebhookMatcher,
        repo: Repository
    ) {
        try {
            var page = 1
            val size = 200
            while (true) {
                val webhookCommitList = matcher.getWebhookCommitList(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repository = repo,
                    page = page,
                    size = size
                )
                logger.info("commit list is $webhookCommitList")
                pipelineBuildCommitsDao.create(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    webhookCommits = webhookCommitList,
                    mrId = matcher.getMergeRequestId()?.toString() ?: ""
                )
                if (webhookCommitList.size < size) break
                page++
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildCommitsFinishEvent(
                    source = "build_commits",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
            )
        } catch (ignore: Throwable) {
            logger.info("save build info err | err is $ignore")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildCommitsService::class.java)
    }
}
