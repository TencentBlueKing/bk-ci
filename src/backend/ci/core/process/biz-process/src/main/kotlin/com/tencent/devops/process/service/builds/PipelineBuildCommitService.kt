package com.tencent.devops.process.service.builds

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCommitFinishEvent
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.dao.PipelineBuildCommitDao
import com.tencent.devops.process.pojo.code.PipelineBuildCommit
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.repository.pojo.Repository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildCommitService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBuildCommitDao: PipelineBuildCommitDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val client: Client
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
                webhookCommitList.forEach {
                    pipelineBuildCommitDao.create(
                        dslContext = dslContext,
                        id = client.get(ServiceAllocIdResource::class)
                            .generateSegmentId("PIPELINE_BUILD_COMMITS").data,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        commitId = it.commitId,
                        authorName = it.authorName,
                        message = it.message,
                        repoType = it.repoType,
                        url = repo.url,
                        eventType = it.eventType,
                        commitTime = it.commitTime,
                        mrId = it.mrId ?: "",
                        channel = ChannelCode.BS.name,
                        action = it.action ?: ""
                    )
                }
                if (webhookCommitList.size < size) break
                page++
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildCommitFinishEvent(
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

    fun saveCommits(commits: List<PipelineBuildCommit>) {
        commits.forEach { buildCommit ->
            with(buildCommit) {
                pipelineBuildCommitDao.create(
                    dslContext = dslContext,
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PIPELINE_BUILD_COMMITS").data,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    commitId = commitId,
                    authorName = authorName,
                    message = message,
                    repoType = repoType,
                    url = url,
                    eventType = eventType,
                    commitTime = commitTime,
                    mrId = mrId ?: "",
                    channel = channel,
                    action = action ?: ""
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildCommitService::class.java)
    }
}
