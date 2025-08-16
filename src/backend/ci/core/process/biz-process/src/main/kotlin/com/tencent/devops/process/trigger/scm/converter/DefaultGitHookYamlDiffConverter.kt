package com.tencent.devops.process.trigger.scm.converter

import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.yaml.PipelineYamlFileService
import com.tencent.devops.process.yaml.actions.GitActionCommon
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
class DefaultGitHookYamlDiffConverter @Autowired constructor(
    private val pipelineYamlFileService: PipelineYamlFileService
) : WebhookYamlDiffConverter {
    override fun support(webhook: Webhook): Boolean {
        return webhook is IssueHook || webhook is AbstractCommentHook || webhook is PullRequestReviewHook
    }

    override fun convert(
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ): List<PipelineYamlDiff> {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val serverRepo = webhook.repository() as GitScmServerRepository
        val defaultBranch = serverRepo.defaultBranch!!
        val fileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = defaultBranch,
            authRepository = AuthRepository(repository)
        )
        return fileTrees.map { tree ->
            val filePath = GitActionCommon.getCiFilePath(tree.path)
            PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = webhook.eventType,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                filePath = filePath,
                fileType = YamlFileType.getFileType(filePath),
                actionType = YamlFileActionType.TRIGGER,
                triggerUser = webhook.userName,
                ref = defaultBranch,
                blobId = tree.blobId,
            )
        }
    }
}
