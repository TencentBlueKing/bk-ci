package com.tencent.devops.process.trigger.scm.converter

import com.tencent.devops.process.yaml.PipelineYamlFileService
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.process.yaml.pojo.YamlFileActionType
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.enums.EventAction
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.GitTagHook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitTagHookConverter @Autowired constructor(
    private val pipelineYamlFileService: PipelineYamlFileService
) : WebhookConverter {
    override fun support(webhook: Webhook): Boolean {
        return webhook is GitTagHook
    }

    override fun convert(
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ): List<PipelineYamlFileEvent> {
        webhook as GitTagHook
        // 删除TAG暂不处理
        if (webhook.action == EventAction.DELETE) {
            return listOf()
        }
        val projectId = repository.projectId!!
        val tag = webhook.ref.name
        val fileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = tag,
            authRepository = AuthRepository(repository)
        )
        val serverRepo = webhook.repo as GitScmServerRepository
        return fileTrees.map { tree ->
            val filePath = GitActionCommon.getCiFilePath(tree.path)
            PipelineYamlFileEvent(
                userId = webhook.sender.name,
                authUser = repository.userName,
                projectId = projectId,
                eventId = eventId,
                repository = repository,
                defaultBranch = serverRepo.defaultBranch,
                actionType = YamlFileActionType.TRIGGER,
                filePath = filePath,
                ref = tag,
                blobId = tree.blobId,
                authRepository = AuthRepository(repository),
                fork = false
            )
        }
    }
}
