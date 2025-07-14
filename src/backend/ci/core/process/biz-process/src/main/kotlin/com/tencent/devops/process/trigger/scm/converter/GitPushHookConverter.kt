/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.trigger.scm.converter

import com.tencent.devops.process.yaml.PipelineYamlFileService
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.mq.FileCommit
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.process.yaml.pojo.YamlFileActionType
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.enums.EventAction
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.GitPushHook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GitPushHookConverter @Autowired constructor(
    private val pipelineYamlFileService: PipelineYamlFileService
) : WebhookConverter {
    override fun support(webhook: Webhook): Boolean {
        return webhook is GitPushHook
    }

    override fun convert(
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ): List<PipelineYamlFileEvent> {
        webhook as GitPushHook
        return if (webhook.action == EventAction.DELETE) {
            getDeleteYamlFileEvent(
                eventId = eventId,
                repository = repository,
                webhook = webhook
            )
        } else {
            getNotDeleteYamlFileEvent(
                eventId = eventId,
                repository = repository,
                webhook = webhook
            )
        }
    }

    private fun getNotDeleteYamlFileEvent(
        eventId: Long,
        repository: Repository,
        webhook: GitPushHook
    ): List<PipelineYamlFileEvent> {
        val projectId = repository.projectId!!
        val ref = webhook.ref
        val fileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = ref,
            authRepository = AuthRepository(repository)
        )

        val serverRepo = webhook.repo
        val changeFiles = WebhookConverterUtils.getChangeFiles(webhook.changes)
        val yamlFileEvents = mutableListOf<PipelineYamlFileEvent>()
        val defaultBranch = serverRepo.defaultBranch!!
        fileTrees.forEach { tree ->
            val filePath = GitActionCommon.getCiFilePath(tree.path)
            val actionType = WebhookConverterUtils.getYamlActionType(filePath = filePath, changeFiles = changeFiles)
            val oldFilePath = changeFiles.renamedFiles[filePath]
            val yamlFileEvent = PipelineYamlFileEvent(
                userId = webhook.sender.name,
                authUser = repository.userName,
                projectId = projectId,
                eventId = eventId,
                repository = repository,
                defaultBranch = defaultBranch,
                actionType = actionType,
                filePath = filePath,
                oldFilePath = oldFilePath,
                ref = ref,
                blobId = tree.blobId,
                authRepository = AuthRepository(repository),
                commit = FileCommit(
                    commitId = webhook.commit?.sha ?: "",
                    commitMsg = webhook.commit?.message ?: "",
                    commitTime = webhook.commit?.commitTime ?: LocalDateTime.now(),
                    committer = webhook.commit?.committer?.name ?: ""
                )
            )
            yamlFileEvents.add(yamlFileEvent)
        }
        // yaml文件删除
        changeFiles.deletedFiles.filter { GitActionCommon.isCiFile(it) }.forEach { filePath ->
            val yamlFileEvent = PipelineYamlFileEvent(
                userId = webhook.sender.name,
                authUser = repository.userName,
                projectId = projectId,
                eventId = eventId,
                repository = repository,
                defaultBranch = defaultBranch,
                ref = ref,
                filePath = filePath,
                actionType = YamlFileActionType.DELETE,
                fork = false
            )
            yamlFileEvents.add(yamlFileEvent)
        }
        return yamlFileEvents
    }

    private fun getDeleteYamlFileEvent(
        eventId: Long,
        repository: Repository,
        webhook: GitPushHook
    ): List<PipelineYamlFileEvent> {
        val projectId = repository.projectId!!
        val ref = webhook.ref
        val serverRepo = webhook.repo
        val filePaths = pipelineYamlFileService.getAllBranchFilePath(
            projectId = projectId,
            repoHashId = repository.repoHashId!!,
            branch = ref
        )
        val yamlFileEvents = mutableListOf<PipelineYamlFileEvent>()
        filePaths.forEach { filePath ->
            val yamlFileEvent = PipelineYamlFileEvent(
                userId = webhook.sender.name,
                authUser = repository.userName,
                projectId = projectId,
                eventId = eventId,
                repository = repository,
                defaultBranch = serverRepo.defaultBranch!!,
                ref = ref,
                filePath = filePath,
                actionType = YamlFileActionType.DELETE,
                fork = false
            )
            yamlFileEvents.add(yamlFileEvent)
        }
        return yamlFileEvents
    }
}
