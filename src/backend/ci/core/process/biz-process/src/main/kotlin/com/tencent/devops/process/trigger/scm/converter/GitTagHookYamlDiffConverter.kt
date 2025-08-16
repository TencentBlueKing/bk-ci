package com.tencent.devops.process.trigger.scm.converter

import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.yaml.PipelineYamlFileService
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.enums.EventAction
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.GitTagHook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitTagHookYamlDiffConverter @Autowired constructor(
    private val pipelineYamlFileService: PipelineYamlFileService
) : WebhookYamlDiffConverter {
    override fun support(webhook: Webhook): Boolean {
        return webhook is GitTagHook
    }

    override fun convert(
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ): List<PipelineYamlDiff> {
        webhook as GitTagHook
        // 删除TAG暂不处理
        if (webhook.action == EventAction.DELETE) {
            return listOf()
        }
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val tag = webhook.ref.name
        val fileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = tag,
            authRepository = AuthRepository(repository)
        )
        val serverRepo = webhook.repo
        val defaultBranch = serverRepo.defaultBranch!!
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
                triggerUser = webhook.sender.name,
                ref = tag,
                blobId = tree.blobId
            )
        }
    }
}
