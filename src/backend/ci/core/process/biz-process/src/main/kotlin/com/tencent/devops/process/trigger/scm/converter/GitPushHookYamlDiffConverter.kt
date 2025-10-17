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

import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.yaml.PipelineYamlFileService
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.enums.EventAction
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.GitPushHook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GitPushHookYamlDiffConverter @Autowired constructor(
    private val pipelineYamlFileService: PipelineYamlFileService
) : WebhookYamlDiffConverter {
    override fun support(webhook: Webhook): Boolean {
        return webhook is GitPushHook
    }

    override fun convert(
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ): List<PipelineYamlDiff> {
        webhook as GitPushHook
        return if (webhook.action == EventAction.DELETE) {
            getBranchDeleteYamlDiffs(
                eventId = eventId,
                repository = repository,
                webhook = webhook
            )
        } else {
            getNotBranchDeleteYamlDiffs(
                eventId = eventId,
                repository = repository,
                webhook = webhook
            )
        }
    }

    private fun getNotBranchDeleteYamlDiffs(
        eventId: Long,
        repository: Repository,
        webhook: GitPushHook
    ): List<PipelineYamlDiff> {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val ref = webhook.ref
        val fileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = ref,
            authRepository = AuthRepository(repository)
        )

        val serverRepo = webhook.repo
        val changeFiles = WebhookConverterUtils.getChangeFiles(webhook.changes)
        val yamlDiffs = mutableListOf<PipelineYamlDiff>()
        val defaultBranch = serverRepo.defaultBranch!!
        fileTrees.forEach { tree ->
            val filePath = GitActionCommon.getCiFilePath(tree.path)
            val actionType = WebhookConverterUtils.getYamlActionType(filePath = filePath, changeFiles = changeFiles)
            // 如果文件类型不能执行,那么没有变更时,直接跳过,比如模版文件,只需要变更,不需要执行
            if (actionType == YamlFileActionType.TRIGGER && !YamlFileType.getFileType(filePath).canExecute()) {
                return@forEach
            }
            val oldFilePath = changeFiles.renamedFiles[filePath]
            val yamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = webhook.eventType,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                filePath = filePath,
                fileType = YamlFileType.getFileType(filePath),
                actionType = actionType,
                triggerUser = webhook.userName,
                oldFilePath = oldFilePath,
                ref = ref,
                blobId = tree.blobId,
                commitId = webhook.commit?.sha ?: "",
                commitMsg = webhook.commit?.message ?: "",
                commitTime = webhook.commit?.commitTime ?: LocalDateTime.now(),
                committer = webhook.commit?.committer?.name ?: ""
            )
            yamlDiffs.add(yamlDiff)
        }
        // yaml文件删除
        changeFiles.deletedFiles.filter { GitActionCommon.isCiFile(it) }.forEach { filePath ->
            val yamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = webhook.eventType,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                filePath = filePath,
                fileType = YamlFileType.getFileType(filePath),
                actionType = YamlFileActionType.DELETE,
                triggerUser = webhook.userName,
                ref = ref
            )
            yamlDiffs.add(yamlDiff)
        }
        return yamlDiffs
    }

    private fun getBranchDeleteYamlDiffs(
        eventId: Long,
        repository: Repository,
        webhook: GitPushHook
    ): List<PipelineYamlDiff> {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val ref = webhook.ref
        val serverRepo = webhook.repo
        val filePaths = pipelineYamlFileService.getAllBranchFilePath(
            projectId = projectId,
            repoHashId = repository.repoHashId!!,
            branch = ref
        )
        val yamlDiffs = mutableListOf<PipelineYamlDiff>()
        filePaths.forEach { filePath ->
            val yamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = webhook.eventType,
                repoHashId = repoHashId,
                defaultBranch = serverRepo.defaultBranch!!,
                ref = ref,
                filePath = filePath,
                fileType = YamlFileType.getFileType(filePath),
                actionType = YamlFileActionType.DELETE,
                triggerUser = webhook.userName,
            )
            yamlDiffs.add(yamlDiff)
        }
        return yamlDiffs
    }
}
