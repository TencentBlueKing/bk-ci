package com.tencent.devops.process.trigger.scm.converter

import com.tencent.devops.process.yaml.PipelineYamlFileService
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.process.yaml.pojo.YamlFileActionType
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.AbstractCommentHook
import com.tencent.devops.scm.api.pojo.webhook.git.IssueHook
import com.tencent.devops.scm.api.pojo.webhook.git.PullRequestReviewHook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DefaultGitHookConverter @Autowired constructor(
    private val pipelineYamlFileService: PipelineYamlFileService
) : WebhookConverter {
    override fun support(webhook: Webhook): Boolean {
        return webhook is IssueHook || webhook is AbstractCommentHook || webhook is PullRequestReviewHook
    }

    override fun convert(
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ): List<PipelineYamlFileEvent> {
        val projectId = repository.projectId!!
        val serverRepo = webhook.repository() as GitScmServerRepository
        val defaultBranch = serverRepo.defaultBranch!!
        val fileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = defaultBranch,
            authRepository = AuthRepository(repository)
        )
        return fileTrees.map { tree ->
            val filePath = GitActionCommon.getCiFilePath(tree.path)
            PipelineYamlFileEvent(
                userId = webhook.userName,
                authUser = repository.userName,
                projectId = projectId,
                eventId = eventId,
                repository = repository,
                defaultBranch = defaultBranch,
                actionType = YamlFileActionType.TRIGGER,
                filePath = filePath,
                ref = defaultBranch,
                blobId = tree.blobId,
                authRepository = AuthRepository(repository),
                fork = false
            )
        }
    }
}
