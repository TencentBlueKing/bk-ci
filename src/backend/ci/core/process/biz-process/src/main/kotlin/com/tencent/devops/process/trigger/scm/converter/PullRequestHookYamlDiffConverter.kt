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
import com.tencent.devops.repository.pojo.credential.UserOauthTokenAuthCred
import com.tencent.devops.scm.api.enums.EventAction
import com.tencent.devops.scm.api.pojo.Tree
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.api.pojo.webhook.git.PullRequestHook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PullRequestHookYamlDiffConverter @Autowired constructor(
    private val pipelineYamlFileService: PipelineYamlFileService
) : WebhookYamlDiffConverter {
    override fun support(webhook: Webhook): Boolean {
        return webhook is PullRequestHook
    }

    override fun convert(
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ): List<PipelineYamlDiff> {
        webhook as PullRequestHook
        return when (webhook.action) {
            EventAction.MERGE -> {
                getMergedYamlDiffs(eventId = eventId, repository = repository, hook = webhook)
            }

            EventAction.CLOSE -> {
                getClosedYamlDiffs(eventId = eventId, repository = repository, hook = webhook)
            }

            else -> {
                getNotMergeYamlDiffs(eventId = eventId, repository = repository, hook = webhook)
            }
        }
    }

    private fun getNotMergeYamlDiffs(
        eventId: Long,
        repository: Repository,
        hook: PullRequestHook
    ): List<PipelineYamlDiff> {
        val projectId = repository.projectId!!
        val pullRequest = hook.pullRequest

        val sourceRepo = pullRequest.sourceRepo
        val targetRepo = pullRequest.targetRepo
        val fork = sourceRepo.id != targetRepo.id

        val targetFileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = pullRequest.targetRef.name,
            authRepository = AuthRepository(repository)
        )
        val sourceFileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = pullRequest.sourceRef.name,
            authRepository = if (fork) {
                // fork仓库,需要获取pr触发人的oauth获取文件信息
                AuthRepository(
                    scmCode = repository.scmCode,
                    url = sourceRepo.httpUrl,
                    userName = repository.userName,
                    auth = UserOauthTokenAuthCred(hook.sender.name)
                )
            } else {
                AuthRepository(repository)
            }
        )
        return computeNotMergeYamlDiffs(
            eventId = eventId,
            repository = repository,
            hook = hook,
            targetFileTrees = targetFileTrees,
            sourceFileTrees = sourceFileTrees
        )
    }

    /**
     * 对比源分支和目标分支的文件,确定使用哪个分支的文件触发
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun computeNotMergeYamlDiffs(
        eventId: Long,
        repository: Repository,
        hook: PullRequestHook,
        targetFileTrees: List<Tree>,
        sourceFileTrees: List<Tree>
    ): List<PipelineYamlDiff> {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val pullRequest = hook.pullRequest
        val serverRepo = hook.repo
        val sourceRepo = pullRequest.sourceRepo
        val targetRepo = pullRequest.targetRepo

        val sourceBranch = pullRequest.sourceRef.name
        val targetBranch = pullRequest.targetRef.name
        val defaultBranch = serverRepo.defaultBranch!!
        val fork = sourceRepo.id != targetRepo.id

        val targetFilePaths = targetFileTrees.map { GitActionCommon.getCiFilePath(it.path) }
        val sourceFilePaths = sourceFileTrees.map { GitActionCommon.getCiFilePath(it.path) }

        val changeFiles = WebhookConverterUtils.getChangeFiles(hook.changes)

        val yamlDiffs = mutableListOf<PipelineYamlDiff>()
        sourceFileTrees.forEach { sourceTree ->
            val sourcePath = GitActionCommon.getCiFilePath(sourceTree.path)
            val baseYamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = hook.eventType,
                repoHashId = repoHashId,
                actionType = YamlFileActionType.CREATE,
                defaultBranch = defaultBranch,
                filePath = sourcePath,
                fileType = YamlFileType.getFileType(sourcePath),
                triggerUser = hook.userName,
                ref = GitActionCommon.getSourceRef(
                    fork = fork,
                    sourceFullName = sourceRepo.fullName,
                    sourceBranch = sourceBranch
                ),
                commitId = hook.commit.sha,
                commitMsg = hook.commit.message,
                commitTime = hook.commit.commitTime ?: LocalDateTime.now(),
                committer = hook.commit.committer?.name ?: "",
                blobId = sourceTree.blobId,
                fork = fork,
                useForkToken = true,
                pullRequestId = pullRequest.id,
                pullRequestNumber = pullRequest.number,
                pullRequestUrl = pullRequest.link,
                sourceBranch = sourceBranch,
                targetBranch = targetBranch,
                sourceRepoUrl = sourceRepo.httpUrl,
                sourceFullName = sourceRepo.fullName,
                targetRepoUrl = targetRepo.httpUrl,
                targetFullName = targetRepo.fullName
            )
            when {
                // 源分支有，目标分支没有，新增列表或更新列表有，说明源分支新增,以源分支为主
                // mr获取的对比文件,源分支是新增,但是在mr变更这里可能是更新
                sourcePath !in targetFilePaths &&
                    (sourcePath in changeFiles.addedFiles || sourcePath in changeFiles.updatedFiles) -> {
                    val yamlDiff = baseYamlDiff.copy(actionType = YamlFileActionType.CREATE)
                    yamlDiffs.add(yamlDiff)
                }
                // 源分支有，目标分支没有，重命名列表有，说明源分支重命名,以源分支为主
                sourcePath !in targetFilePaths && sourcePath in changeFiles.renamedFiles -> {
                    val yamlDiff = baseYamlDiff.copy(
                        actionType = YamlFileActionType.RENAME,
                        oldFilePath = changeFiles.renamedFiles[sourcePath]
                    )
                    yamlDiffs.add(yamlDiff)
                }
                // 源分支有，目标分支没有，变更列表没有，说明目标分支被删除,不触发
                sourcePath !in targetFilePaths && sourcePath !in changeFiles.allFiles -> {
                    return@forEach
                }
                // 源分支有，目标分支有，变更列表有，需要对版本进行预合并 // TODO 如果有预合并,推荐使用预合并commitId
                sourcePath in targetFilePaths && sourcePath in changeFiles.updatedFiles -> {
                    val yamlDiff = baseYamlDiff.copy(actionType = YamlFileActionType.UPDATE)
                    yamlDiffs.add(yamlDiff)
                }
            }
        }
        targetFileTrees.forEach { targetTree ->
            val targetPath = GitActionCommon.getCiFilePath(targetTree.path)
            if (targetPath in yamlDiffs.map { it.filePath }.toSet()) {
                return@forEach
            }
            val baseYamlDiff = PipelineYamlDiff(
                projectId = repository.projectId!!,
                eventId = eventId,
                eventType = hook.eventType,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                ref = pullRequest.targetRef.name,
                filePath = targetPath,
                fileType = YamlFileType.getFileType(targetPath),
                triggerUser = hook.userName,
                actionType = YamlFileActionType.TRIGGER,
                blobId = targetTree.blobId,
                commitId = hook.commit.sha,
                commitMsg = hook.commit.message,
                commitTime = hook.commit.commitTime ?: LocalDateTime.now(),
                committer = hook.commit.committer?.name ?: "",
                fork = fork,
                useForkToken = false,
                pullRequestId = pullRequest.id,
                pullRequestNumber = pullRequest.number,
                pullRequestUrl = pullRequest.link,
                sourceBranch = pullRequest.sourceRef.name,
                targetBranch = pullRequest.targetRef.name,
                sourceRepoUrl = sourceRepo.httpUrl,
                sourceFullName = sourceRepo.fullName,
                targetRepoUrl = targetRepo.httpUrl,
                targetFullName = targetRepo.fullName
            )
            when {
                // 源分支没有，目标分支有，删除列表有，说明是删除,需要删除
                targetPath !in sourceFilePaths && targetPath in changeFiles.deletedFiles -> {
                    val yamlDiff = baseYamlDiff.copy(
                        ref = GitActionCommon.getSourceRef(
                            fork = fork,
                            sourceFullName = sourceRepo.fullName,
                            sourceBranch = sourceBranch
                        ),
                        actionType = YamlFileActionType.DELETE,
                        blobId = null
                    )
                    yamlDiffs.add(yamlDiff)
                }
                // 源分支没有，目标分支有，重命名列表有，说明是重命名,重命名在源分支那已处理,不需要再处理
                targetPath !in sourceFilePaths && targetPath in changeFiles.renamedOldFiles -> {
                    return@forEach
                }
                // 源分支没有，目标分支有，删除列表没有，说明是目标分支新增的,需要触发
                targetPath !in sourceFilePaths && targetPath !in changeFiles.deletedFiles -> {
                    // 如果文件类型不能执行,那么没有变更时,直接跳过,比如模版文件,只需要变更,不需要执行
                    if (!YamlFileType.getFileType(targetPath).canExecute()) {
                        return@forEach
                    }
                    yamlDiffs.add(baseYamlDiff)
                }
                // 源分支有，目标分支有，变更列表无，以目标分支为主，不需要校验版本
                targetPath in sourceFilePaths && targetPath !in changeFiles.allFiles -> {
                    // 如果文件类型不能执行,那么没有变更时,直接跳过,比如模版文件,只需要变更,不需要执行
                    if (!YamlFileType.getFileType(targetPath).canExecute()) {
                        return@forEach
                    }
                    yamlDiffs.add(baseYamlDiff)
                }
            }
        }
        return yamlDiffs
    }

    private fun getMergedYamlDiffs(
        eventId: Long,
        repository: Repository,
        hook: PullRequestHook
    ): List<PipelineYamlDiff> {
        val projectId = repository.projectId!!
        val pullRequest = hook.pullRequest

        val targetFileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = pullRequest.targetRef.name,
            authRepository = AuthRepository(repository)
        )

        return computeMergedYamlDiffs(
            eventId = eventId,
            repository = repository,
            hook = hook,
            targetFileTrees = targetFileTrees
        )
    }

    /**
     * 计算获取已经合入的yaml文件事件
     */
    private fun computeMergedYamlDiffs(
        eventId: Long,
        repository: Repository,
        hook: PullRequestHook,
        targetFileTrees: List<Tree>
    ): List<PipelineYamlDiff> {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val pullRequest = hook.pullRequest

        val serverRepo = hook.repo
        val sourceRepo = pullRequest.sourceRepo
        val targetRepo = pullRequest.targetRepo

        val sourceBranch = pullRequest.sourceRef.name
        val targetBranch = pullRequest.targetRef.name
        val defaultBranch = serverRepo.defaultBranch!!
        val fork = sourceRepo.id != targetRepo.id

        val changeFiles = WebhookConverterUtils.getChangeFiles(hook.changes)

        val yamlDiffs = mutableListOf<PipelineYamlDiff>()
        // 目标文件变更事件
        targetFileTrees.forEach { targetTree ->
            val targetPath = GitActionCommon.getCiFilePath(targetTree.path)
            val baseYamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = hook.eventType,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                filePath = targetPath,
                fileType = YamlFileType.getFileType(targetPath),
                actionType = YamlFileActionType.TRIGGER,
                triggerUser = hook.userName,
                ref = targetBranch,
                blobId = targetTree.blobId,
                commitId = hook.commit.sha,
                commitMsg = hook.commit.message,
                commitTime = hook.commit.commitTime ?: LocalDateTime.now(),
                committer = hook.commit.committer?.name ?: "",
                fork = fork,
                pullRequestId = pullRequest.id,
                pullRequestNumber = pullRequest.number,
                pullRequestUrl = pullRequest.link,
                merged = true,
                sourceBranch = sourceBranch,
                targetBranch = targetBranch,
                sourceRepoUrl = sourceRepo.httpUrl,
                sourceFullName = sourceRepo.fullName
            )
            when (targetPath) {
                in changeFiles.addedFiles -> {
                    val yamlDiff = baseYamlDiff.copy(actionType = YamlFileActionType.CREATE)
                    yamlDiffs.add(yamlDiff)
                }

                in changeFiles.updatedFiles -> {
                    val yamlDiff = baseYamlDiff.copy(actionType = YamlFileActionType.UPDATE)
                    yamlDiffs.add(yamlDiff)
                }

                in changeFiles.renamedFiles -> {
                    val yamlDiff = baseYamlDiff.copy(
                        actionType = YamlFileActionType.RENAME,
                        oldFilePath = changeFiles.renamedFiles[targetPath]
                    )
                    yamlDiffs.add(yamlDiff)
                }

                else -> {
                    // 如果文件类型不能执行,那么没有变更时,直接跳过,比如模版文件,只需要变更,不需要执行
                    if (!YamlFileType.getFileType(targetPath).canExecute()) {
                        return@forEach
                    }
                    yamlDiffs.add(baseYamlDiff)
                }
            }
        }
        // 目标文件删除事件
        changeFiles.deletedFiles.filter { GitActionCommon.isCiFile(it) }.forEach { filePath ->
            val yamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = hook.eventType,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                ref = targetBranch,
                filePath = filePath,
                fileType = YamlFileType.getFileType(filePath),
                actionType = YamlFileActionType.DELETE,
                triggerUser = hook.userName,
                fork = fork,
                pullRequestId = pullRequest.id,
                pullRequestNumber = pullRequest.number,
                pullRequestUrl = pullRequest.link,
                merged = true,
                sourceBranch = sourceBranch,
                targetBranch = targetBranch,
                sourceRepoUrl = sourceRepo.httpUrl,
                sourceFullName = sourceRepo.fullName
            )
            yamlDiffs.add(yamlDiff)
        }
        return yamlDiffs
    }

    private fun getClosedYamlDiffs(
        eventId: Long,
        repository: Repository,
        hook: PullRequestHook
    ): List<PipelineYamlDiff> {
        val yamlDiffs = mutableListOf<PipelineYamlDiff>()
        computeClosedDefaultYamlDiff(
            eventId = eventId,
            repository = repository,
            hook = hook,
            yamlDiffs = yamlDiffs
        )
        computeClosedSourceYamlDiff(
            eventId = eventId,
            repository = repository,
            hook = hook,
            yamlDiffs = yamlDiffs
        )
        return yamlDiffs
    }

    /**
     * 计算关闭合并请求的源分支的yaml文件事件
     *
     * 源分支上如果有创建和更新,需要对yaml文件进行关闭操作
     */
    private fun computeClosedSourceYamlDiff(
        eventId: Long,
        repository: Repository,
        hook: PullRequestHook,
        yamlDiffs: MutableList<PipelineYamlDiff>
    ) {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val serverRepo = hook.repository()
        val pullRequest = hook.pullRequest

        val sourceRepo = pullRequest.sourceRepo
        val targetRepo = pullRequest.targetRepo
        val fork = sourceRepo.id != targetRepo.id
        val defaultBranch = serverRepo.defaultBranch!!
        val sourceBranch = pullRequest.sourceRef.name
        val targetBranch = pullRequest.targetRef.name

        // 如果不是fork仓库,并且是默认分支,那么不需要触发
        if (!fork && defaultBranch == sourceBranch) {
            return
        }
        val sourceFileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = pullRequest.sourceRef.name,
            authRepository = if (fork) {
                // fork仓库,需要获取pr触发人的oauth获取文件信息
                AuthRepository(
                    scmCode = repository.scmCode,
                    url = sourceRepo.httpUrl,
                    userName = repository.userName,
                    auth = UserOauthTokenAuthCred(hook.sender.name)
                )
            } else {
                AuthRepository(repository)
            }
        )
        val changeFiles = WebhookConverterUtils.getChangeFiles(hook.changes)

        sourceFileTrees.forEach { sourceTree ->
            val sourcePath = GitActionCommon.getCiFilePath(sourceTree.path)
            val yamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = hook.eventType,
                repoHashId = repoHashId,
                actionType = YamlFileActionType.CLOSE,
                defaultBranch = defaultBranch,
                filePath = sourcePath,
                fileType = YamlFileType.getFileType(sourcePath),
                triggerUser = hook.userName,
                ref = GitActionCommon.getSourceRef(
                    fork = fork,
                    sourceFullName = sourceRepo.fullName,
                    sourceBranch = sourceBranch
                ),
                commitId = hook.commit.sha,
                commitMsg = hook.commit.message,
                commitTime = hook.commit.commitTime ?: LocalDateTime.now(),
                committer = hook.commit.committer?.name ?: "",
                blobId = sourceTree.blobId,
                fork = fork,
                useForkToken = true,
                pullRequestId = pullRequest.id,
                pullRequestNumber = pullRequest.number,
                pullRequestUrl = pullRequest.link,
                sourceBranch = sourceBranch,
                targetBranch = targetBranch,
                sourceRepoUrl = sourceRepo.httpUrl,
                sourceFullName = sourceRepo.fullName,
                targetRepoUrl = targetRepo.httpUrl,
                targetFullName = targetRepo.fullName
            )
            if (changeFiles.addedFiles.contains(sourcePath) ||
                changeFiles.updatedFiles.contains(sourcePath)
            ) {
                yamlDiffs.add(yamlDiff)
            }
        }
    }

    private fun computeClosedDefaultYamlDiff(
        eventId: Long,
        repository: Repository,
        hook: PullRequestHook,
        yamlDiffs: MutableList<PipelineYamlDiff>
    ) {
        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val serverRepo = hook.repository()
        val pullRequest = hook.pullRequest

        val sourceRepo = pullRequest.sourceRepo
        val targetRepo = pullRequest.targetRepo
        val fork = sourceRepo.id != targetRepo.id
        val defaultBranch = serverRepo.defaultBranch!!
        val sourceBranch = pullRequest.sourceRef.name
        val targetBranch = pullRequest.targetRef.name

        // 关闭合并请求,需要触发默认分支的yaml文件
        val defaultFileTrees = pipelineYamlFileService.listFileTree(
            projectId = projectId,
            ref = defaultBranch,
            authRepository = AuthRepository(repository)
        )
        defaultFileTrees.forEach { tree ->
            val filePath = GitActionCommon.getCiFilePath(tree.path)
            val yamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = hook.eventType,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                filePath = filePath,
                fileType = YamlFileType.getFileType(filePath),
                actionType = YamlFileActionType.TRIGGER,
                triggerUser = hook.userName,
                ref = defaultBranch,
                blobId = tree.blobId,
                fork = fork,
                pullRequestId = pullRequest.id,
                pullRequestNumber = pullRequest.number,
                pullRequestUrl = pullRequest.link,
                merged = true,
                sourceBranch = sourceBranch,
                targetBranch = targetBranch,
                sourceRepoUrl = sourceRepo.httpUrl,
                sourceFullName = sourceRepo.fullName
            )
            yamlDiffs.add(yamlDiff)
        }
    }
}
