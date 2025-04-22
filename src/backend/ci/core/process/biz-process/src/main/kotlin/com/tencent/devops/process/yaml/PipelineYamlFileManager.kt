/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.ENABLE_PAC_EVENT_DESC
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PAC_DEFAULT_BRANCH_FILE_DELETED
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileSyncReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.pipeline.enums.PipelineYamlStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.scm.listener.PipelineYamlChangeContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.mq.FileCommit
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.process.yaml.pojo.PipelineYamlTriggerLock
import com.tencent.devops.process.yaml.pojo.YamlFileActionType
import com.tencent.devops.process.yaml.pojo.YamlPipelineActionType
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectLoader
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmFileApiResource
import com.tencent.devops.repository.api.scm.ServiceScmPullRequestApiResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.hub.ScmFilePushReq
import com.tencent.devops.repository.pojo.hub.ScmPullRequestCreateReq
import com.tencent.devops.scm.api.enums.ContentKind
import com.tencent.devops.scm.api.pojo.Commit
import com.tencent.devops.scm.api.pojo.Content
import com.tencent.devops.scm.api.pojo.PullRequest
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineYamlFileManager @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val pipelineYamlViewService: PipelineYamlViewService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val pipelineYamlFileService: PipelineYamlFileService,
    private val pipelineYamlResourceManager: PipelineYamlResourceManager,
    private val eventDispatcher: SampleEventDispatcher,
    private val pipelineTriggerEventService: PipelineTriggerEventService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlRepositoryService::class.java)
    }

    fun syncYamlFile(
        userId: String,
        projectId: String,
        yamlFileSyncReq: PipelineYamlFileSyncReq
    ) {
        val repoHashId = yamlFileSyncReq.repository.repoHashId!!
        try {
            val yamlFileEvents = mutableListOf<PipelineYamlFileEvent>()
            with(yamlFileSyncReq) {
                val requestId = MDC.get(TraceTag.BIZID)
                val eventId = pipelineTriggerEventService.getEventId()
                val eventDesc = I18Variable(
                    code = ENABLE_PAC_EVENT_DESC,
                    params = listOf(userId)
                ).toJsonStr()
                val triggerEvent = PipelineTriggerEvent(
                    projectId = repository.projectId,
                    eventId = eventId,
                    triggerType = repository.getScmType().name,
                    eventSource = repository.repoHashId,
                    eventType = PipelineTriggerType.MANUAL.name,
                    triggerUser = userId,
                    eventDesc = eventDesc,
                    requestId = requestId,
                    createTime = LocalDateTime.now(),
                    eventBody = null
                )
                pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)

                fileTrees.filter {
                    it.kind == ContentKind.FILE && GitActionCommon.checkYamlPipelineFile(it.path)
                }.forEach { tree ->
                    val filePath = GitActionCommon.getCiFilePath(tree.path)
                    val oldFilePath = null
                    val yamlFileEvent = PipelineYamlFileEvent(
                        userId = userId,
                        authUser = repository.userName,
                        projectId = projectId,
                        eventId = eventId,
                        repository = repository,
                        defaultBranch = defaultBranch,
                        actionType = YamlFileActionType.SYNC,
                        filePath = filePath,
                        oldFilePath = oldFilePath,
                        ref = defaultBranch,
                        blobId = tree.blobId,
                        authRepository = AuthRepository(repository),
                        commit = FileCommit(
                            commitId = commit.sha,
                            commitMsg = commit.message,
                            commitTime = commit.commitTime,
                            committer = commit.committer.name
                        )
                    )
                    yamlFileEvents.add(yamlFileEvent)
                }

                val directories = yamlFileEvents.map { GitActionCommon.getCiDirectory(it.filePath) }.toSet()
                // 创建yaml流水线组
                pipelineYamlViewService.createYamlViewIfAbsent(
                    userId = userId,
                    projectId = projectId,
                    repoHashId = repoHashId,
                    repoFullName = repository.projectName,
                    directoryList = directories
                )

                yamlFileEvents.forEach {
                    eventDispatcher.dispatch(it)
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed to sync pipeline yaml file|projectId:$projectId|repoHashId:$repoHashId", exception)
            pipelineYamlSyncService.enablePacFailed(
                projectId = projectId,
                repoHashId = repoHashId
            )
            throw exception
        }
    }

    fun createOrUpdateYamlFile(event: PipelineYamlFileEvent): Boolean {
        with(event) {
            checkParam()
            logger.info(
                "[PAC_PIPELINE]|create or update yaml pipeline|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref|" +
                        "commitId:${commit!!.commitId}|blobId:$blobId"
            )
            val lock = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
            val context = PipelineYamlChangeContext(
                projectId = projectId,
                filePath = filePath,
                eventId = eventId,
                actionType = YamlPipelineActionType.CREATE
            )
            return try {
                lock.lock()
                createOrUpdateYamlPipeline(context = context)
                webhookTriggerManager.fireChangeSuccess(context = context)
                true
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to create or update yaml pipeline|eventId:$eventId|" +
                            "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref|" +
                            "commitId:${commit.commitId}|blobId:$blobId",
                    ignored
                )
                webhookTriggerManager.fireChangeError(context = context, exception = ignored)
                false
            } finally {
                lock.unlock()
            }
        }
    }

    /**
     *
     * 1.如果是默认分支删除,直接删除流水线
     * 2.如果是非默认分支删除
     *   - 当前流水线有正式版本,删除分支版本
     *   - 当前事件是mr非合并事件,并且fork仓库,删除分支版本
     *   - 否则删除流水线
     *
     */
    fun deleteYamlFile(event: PipelineYamlFileEvent): Boolean {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|delete pipeline yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref"
            )
            val lock = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
            val context = PipelineYamlChangeContext(
                projectId = projectId,
                filePath = filePath,
                eventId = eventId,
                actionType = YamlPipelineActionType.DELETE
            )
            return try {
                lock.lock()
                deletePipelineOrBranchVersion(context = context)
                webhookTriggerManager.fireChangeSuccess(context = context)
                true
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to delete pipeline yaml|eventId:$eventId|" +
                            "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref",
                    ignored
                )
                webhookTriggerManager.fireChangeError(context = context, exception = ignored)
                false
            } finally {
                lock.unlock()
            }
        }
    }

    /**
     * 先删除源流水线,再创建新流水线
     */
    fun renameYamlFile(event: PipelineYamlFileEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|rename pipeline yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref"
            )
            checkParam()
            if (oldFilePath.isNullOrBlank()) {
                logger.error("old file path cannot be empty")
                return
            }
            val oldFileEvent = event.copy(filePath = oldFilePath)
            if (deleteYamlFile(event = oldFileEvent)) {
                createOrUpdateYamlFile(event = event)
            }
        }
    }

    /**
     * 发布流水线
     *
     */
    fun releaseYamlFile(yamlFileReleaseReq: PipelineYamlFileReleaseReq): PipelineYamlFileReleaseResult {
        with(yamlFileReleaseReq) {
            logger.info(
                "[PAC_PIPELINE]|release pipeline yaml file|" +
                        "userId:$userId|projectId:$projectId|pipelineId:$pipelineId|" +
                        "repoHashId:$repoHashId|version:$version|versionName:$versionName"
            )
            val repository = client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = repoHashId,
                repositoryType = RepositoryType.ID
            ).data ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.GIT_NOT_FOUND,
                params = arrayOf(repoHashId)
            )
            val authRepository = AuthRepository(repository)
            val serverRepository = client.get(ServiceScmRepositoryApiResource::class).getServerRepository(
                projectId = projectId,
                authRepository = authRepository
            ).data
            if (serverRepository !is GitScmServerRepository) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
                )
            }

            val lock = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                expiredTimeInSeconds = 180
            )
            return try {
                lock.lock()
                val defaultBranch = serverRepository.defaultBranch
                val ref = when {
                    targetAction == CodeTargetAction.COMMIT_TO_MASTER -> defaultBranch
                    targetAction == CodeTargetAction.COMMIT_TO_BRANCH && targetBranch == defaultBranch -> defaultBranch
                    else -> versionName!!
                }
                // 推送文件
                val filePushResult = client.get(ServiceScmFileApiResource::class).pushFile(
                    projectId = projectId,
                    filePushReq = ScmFilePushReq(
                        path = filePath,
                        ref = ref,
                        defaultBranch = defaultBranch,
                        content = content,
                        message = commitMessage,
                        authRepository = authRepository
                    )
                ).data!!
                // 创建mr
                val needCreatePullRequest = targetAction == CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE ||
                        targetAction == CodeTargetAction.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
                val pullRequest = if (needCreatePullRequest) {
                    createPullRequest(
                        ref = ref,
                        targetBranch = defaultBranch,
                        commitMessage = commitMessage,
                        pipelineName = pipelineName,
                        newFile = filePushResult.newFile,
                        authRepository = authRepository
                    )
                } else {
                    null
                }

                createOrUpdateYamlPipeline(
                    ref = ref,
                    defaultBranch = defaultBranch,
                    content = filePushResult.content,
                    commit = filePushResult.commit
                )
                PipelineYamlFileReleaseResult(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    branch = ref,
                    mrUrl = pullRequest?.link
                )
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to release yaml pipeline|projectId:$projectId|pipelineId:$pipelineId|" +
                            "repoHashId:$repoHashId|version:$version|versionName:$versionName",
                    ignored
                )
                throw ignored
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineYamlFileReleaseReq.createPullRequest(
        ref: String,
        targetBranch: String,
        commitMessage: String,
        pipelineName: String,
        newFile: Boolean,
        authRepository: AuthRepository
    ): PullRequest? {
        val dateStr = DateTimeUtil.toDateTime(LocalDateTime.now())
        val title = if (newFile) {
            I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_MERGE_YAML_UPDATE_FILE_TITLE,
                params = arrayOf(dateStr, pipelineName),
                language = I18nUtil.getDefaultLocaleLanguage()
            )
        } else {
            I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_MERGE_YAML_CREATE_FILE_TITLE,
                params = arrayOf(dateStr, pipelineName),
                language = I18nUtil.getDefaultLocaleLanguage()
            )
        }
        return client.get(ServiceScmPullRequestApiResource::class).createPullRequestIfAbsent(
            projectId = projectId,
            pullRequestCreateReq = ScmPullRequestCreateReq(
                title = title,
                body = commitMessage,
                sourceBranch = ref,
                targetBranch = targetBranch,
                authRepository = authRepository
            )
        ).data!!
    }

    private fun PipelineYamlFileEvent.checkParam() {
        if (commit == null) {
            logger.error("[PAC_PIPELINE]|commit cannot be empty")
            return
        }
        if (authRepository == null) {
            logger.error("[PAC_PIPELINE]|auth repository cannot be empty")
            return
        }
        if (blobId == null) {
            logger.error("[PAC_PIPELINE]|blobId cannot be empty")
            return
        }
    }

    private fun PipelineYamlFileEvent.createOrUpdateYamlPipeline(context: PipelineYamlChangeContext) {
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        if (pipelineYamlInfo == null) {
            val deployPipelineResult = createYamlPipeline()

            context.pipelineId = deployPipelineResult.pipelineId
            context.pipelineName = deployPipelineResult.pipelineName
            context.versionName = deployPipelineResult.versionName
        } else {
            val pipelineId = pipelineYamlInfo.pipelineId

            context.actionType = YamlPipelineActionType.UPDATE
            context.pipelineId = pipelineId
            val pipelineName = pipelineYamlResourceManager.getPipelineName(
                projectId = projectId,
                pipelineId = pipelineId,
                isTemplate = GitActionCommon.isTemplateFile(filePath)
            ) ?: run {
                throw ErrorCodeException(
                    errorCode = ERROR_PIPELINE_NOT_EXISTS,
                    params = arrayOf(pipelineId)
                )
            }
            context.pipelineName = pipelineName

            updatePipelineIfAbsent(pipelineId = pipelineId)?.let {
                context.versionName = it.versionName
            } ?: run {
                context.actionType = YamlPipelineActionType.NO_CHANGE
            }
            // 如果合并到目标分支或者fork仓库合并,需要将源分支的分支版本删除
            if (merged && (ref == defaultBranch || fork)) {
                deleteSourceWhenMerged(pipelineId = pipelineId)
            }
        }
    }

    private fun PipelineYamlFileReleaseReq.createOrUpdateYamlPipeline(
        ref: String,
        defaultBranch: String,
        content: Content,
        commit: Commit
    ) {
        val directory = GitActionCommon.getCiDirectory(filePath)
        // 保存流水线
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        if (pipelineYamlInfo == null) {
            pipelineYamlService.save(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                directory = directory,
                defaultBranch = defaultBranch,
                blobId = content.blobId!!,
                ref = ref,
                commitId = commit.sha,
                commitTime = commit.commitTime,
                pipelineId = pipelineId,
                status = if (ref == defaultBranch) {
                    PipelineYamlStatus.OK.name
                } else {
                    PipelineYamlStatus.UN_MERGED.name
                },
                version = version,
                userId = userId
            )
        } else {
            pipelineYamlService.update(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = content.blobId,
                commitId = commit.sha,
                commitTime = commit.commitTime,
                ref = ref,
                defaultBranch = defaultBranch,
                pipelineId = pipelineId,
                version = version,
                userId = userId
            )
        }
    }

    private fun PipelineYamlFileEvent.createYamlPipeline(): DeployPipelineResult {
        val isDefaultBranch = ref == defaultBranch
        val directory = GitActionCommon.getCiDirectory(filePath)
        val yamlInfo = PipelineYamlVo(repoHashId = repoHashId, filePath = filePath)
        // 如果不是默认分支,需要判断默认分支是否已经删除,如果删除,不能再创建
        if (!isDefaultBranch) {
            val defaultBranchDeleted = pipelineYamlFileService.getBranchFilePath(
                projectId = projectId,
                repoHashId = repoHashId,
                branch = defaultBranch,
                filePath = filePath,
                includeDeleted = true
            )?.deleted ?: false
            if (defaultBranchDeleted) {
                throw ErrorCodeException(
                    errorCode = ERROR_PAC_DEFAULT_BRANCH_FILE_DELETED,
                    params = arrayOf(filePath)
                )
            }
        }
        val content = pipelineYamlFileService.getFileContent(
            projectId = projectId,
            path = filePath,
            ref = commit!!.commitId,
            authRepository = authRepository!!
        )
        val deployPipelineResult = pipelineYamlResourceManager.createYamlPipeline(
            userId = authUser,
            projectId = projectId,
            yaml = content.content,
            yamlFileName = GitActionCommon.getCiFileName(filePath),
            branchName = ref,
            isDefaultBranch = isDefaultBranch,
            description = commit.commitMsg,
            aspects = PipelineTransferAspectLoader.initByDefaultTriggerOn(defaultRepo = {
                repository.aliasName
            }),
            yamlInfo = yamlInfo,
            isTemplate = GitActionCommon.isTemplateFile(filePath)
        )
        val pipelineId = deployPipelineResult.pipelineId
        val version = deployPipelineResult.version
        pipelineYamlService.save(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            directory = directory,
            defaultBranch = defaultBranch,
            blobId = content.blobId!!,
            ref = ref,
            commitId = commit.commitId,
            commitTime = commit.commitTime,
            pipelineId = pipelineId,
            status = if (isDefaultBranch) {
                PipelineYamlStatus.OK.name
            } else {
                PipelineYamlStatus.UN_MERGED.name
            },
            version = version,
            userId = userId
        )
        pipelineViewGroupService.updateGroupAfterPipelineUpdate(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = deployPipelineResult.pipelineName,
            creator = userId,
            userId = userId
        )
        logger.info(
            "[PAC_PIPELINE]|create pipeline|eventId:$eventId|" +
                    "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref|" +
                    "pipelineId:$pipelineId|version:$version|versionName:${deployPipelineResult.versionName}"
        )
        return deployPipelineResult
    }

    private fun PipelineYamlFileEvent.updatePipelineIfAbsent(pipelineId: String): DeployPipelineResult? {
        val needCreateVersion = shouldCreateVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            commitId = commit!!.commitId,
            blobId = blobId!!,
            defaultBranch = defaultBranch
        )
        return if (needCreateVersion) {
            updateYamlPipeline(pipelineId = pipelineId)
        } else {
            null
        }
    }

    /**
     * 判断是否需要创建流水线新版本
     *
     * 1. commitId在当前分支存在版本,则不创建版本
     * 2. 当前分支有分支版本
     *  - 分支版本最新blobId与文件的blobId不一样,则需要创建分支版本
     * 3. 当前分支没有分支版本
     *  - 文件blobId在默认分支存在版本,说明分支合并了默认分支,则不创建分支版本
     *  - 文件blobId在默认分支不存在版本,说明是在新分支更新的文件,则创建分支版本
     */
    private fun shouldCreateVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        commitId: String,
        blobId: String,
        defaultBranch: String
    ): Boolean {
        val commitVersionExist = checkCommitVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            commitId = commitId
        )
        return if (commitVersionExist) {
            false
        } else {
            checkRefVersion(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                blobId = blobId,
                defaultBranch = defaultBranch
            )
        }
    }

    /**
     * 如果当前commit已经在当前分支存在,则不创建版本
     */
    private fun checkCommitVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        commitId: String
    ): Boolean {
        val pipelineYamlVersion = pipelineYamlService.getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            commitId = commitId,
            branchAction = BranchVersionAction.ACTIVE.name
        )
        return if (pipelineYamlVersion != null) {
            logger.info(
                "[PAC_PIPELINE]|find pipeline yaml version in commit,skip update|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|commitId:$commitId|" +
                        "version:${pipelineYamlVersion.version}"
            )
            true
        } else {
            false
        }
    }

    /**
     * 1. 当前分支有分支版本
     *  - 分支版本最新blobId与文件的blobId不一样,则需要创建分支版本
     * 2. 当前分支没有分支版本
     *  - 文件blobId在默认分支存在版本,说明分支合并了默认分支,则不创建分支版本
     *  - 文件blobId在默认分支不存在版本,说明是在新分支更新的文件,则创建分支版本
     */
    private fun checkRefVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        blobId: String,
        defaultBranch: String
    ): Boolean {
        return pipelineYamlService.getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            branchAction = BranchVersionAction.ACTIVE.name
        )?.let {
            if (it.blobId == blobId) {
                logger.info(
                    "[PAC_PIPELINE]|find pipeline yaml version in current branch,skip update|" +
                            "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|blobId:$blobId|" +
                            "version:${it.version}"
                )
                false
            } else {
                true
            }
        } ?: checkDefaultBranchVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            blobId = blobId,
            defaultBranch = defaultBranch
        )
    }

    /**
     * 1. 如果当前分支等于默认分支,则创建
     * 2. 如果当前分支不等于默认分支,则判断文件blob_id是否已经在默认分支上
     */
    private fun checkDefaultBranchVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        blobId: String,
        defaultBranch: String
    ): Boolean {
        return if (ref == defaultBranch) {
            true
        } else {
            val pipelineYamlVersion = pipelineYamlService.getPipelineYamlVersion(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = defaultBranch,
                blobId = blobId,
                branchAction = BranchVersionAction.ACTIVE.name
            )
            if (pipelineYamlVersion != null) {
                logger.info(
                    "[PAC_PIPELINE]|find pipeline yaml version in default branch,skip update|" +
                            "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|blobId:$blobId|" +
                            "version:${pipelineYamlVersion.version}"
                )
                false
            } else {
                true
            }
        }
    }

    private fun PipelineYamlFileEvent.updateYamlPipeline(pipelineId: String): DeployPipelineResult {
        val yamlInfo = PipelineYamlVo(
            repoHashId = repoHashId,
            filePath = filePath
        )
        val content = pipelineYamlFileService.getFileContent(
            projectId = projectId,
            path = filePath,
            ref = commit!!.commitId,
            authRepository = authRepository!!
        )
        val deployPipelineResult = pipelineYamlResourceManager.updateYamlPipeline(
            userId = authUser,
            projectId = projectId,
            pipelineId = pipelineId,
            yaml = content.content,
            yamlFileName = GitActionCommon.getCiFileName(filePath),
            branchName = ref,
            isDefaultBranch = ref == defaultBranch,
            description = commit.commitMsg,
            aspects = PipelineTransferAspectLoader.initByDefaultTriggerOn(defaultRepo = {
                repository.aliasName
            }),
            yamlInfo = yamlInfo,
            isTemplate = GitActionCommon.isTemplateFile(filePath)
        )
        pipelineYamlService.update(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            blobId = content.blobId,
            commitId = commit.commitId,
            commitTime = commit.commitTime,
            ref = ref,
            defaultBranch = defaultBranch,
            pipelineId = deployPipelineResult.pipelineId,
            version = deployPipelineResult.version,
            userId = userId
        )
        logger.info(
            "[PAC_PIPELINE]|update pipeline version|eventId:$eventId|" +
                    "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref|" +
                    "version:${deployPipelineResult.version}|versionName:${deployPipelineResult.versionName}"
        )
        return deployPipelineResult
    }

    private fun PipelineYamlFileEvent.deletePipelineOrBranchVersion(context: PipelineYamlChangeContext) {
        logger.info(
            "[PAC_PIPELINE]|delete pipeline or branch version|eventId:$eventId|" +
                    "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref"
        )
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        ) ?: run {
            logger.info(
                "[PAC_PIPELINE]|yaml pipeline not found|projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath"
            )
            context.actionType = YamlPipelineActionType.NO_CHANGE
            return
        }
        val pipelineId = pipelineYamlInfo.pipelineId
        val pipelineName = pipelineYamlResourceManager.getPipelineName(
            projectId = projectId,
            pipelineId = pipelineId,
            isTemplate = GitActionCommon.isTemplateFile(filePath)
        ) ?: run {
            throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
        }
        context.pipelineId = pipelineId
        context.pipelineName = pipelineName
        context.versionName = ref

        // 判断是否能够删除流水线还是删除流水线分支版本
        val (shouldDeletePipeline, shouldDeleteVersion) = shouldDeletePipelineOrVersion(pipelineId = pipelineId)
        if (shouldDeletePipeline) {
            deletePipeline(pipelineId = pipelineId)
        } else {
            if (shouldDeleteVersion) {
                context.actionType = YamlPipelineActionType.DELETE_VERSION
                deleteBranchVersion(pipelineId = pipelineId)
            } else {
                context.actionType = YamlPipelineActionType.NO_CHANGE
                "[PAC_PIPELINE]|branch version has deleted|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref"
            }
        }
        if (shouldDeletePipeline || shouldDeleteVersion) {
            // 默认分支软删除,为了判断默认分支删除后,流水线已被删除,其他分支又修改,导致又创建新的流水线
            pipelineYamlFileService.deleteBranchFile(
                projectId = projectId,
                repoHashId = repoHashId,
                branch = ref,
                filePath = filePath,
                softDelete = ref == defaultBranch
            )
        }
    }

    private fun PipelineYamlFileEvent.shouldDeletePipelineOrVersion(pipelineId: String): Pair<Boolean, Boolean> {
        // 如果是默认分支,则直接删除
        if (ref == defaultBranch) {
            return Pair(true, false)
        }
        // 如果不是默认分支,则判断流水线是否有正式版本,有正式版本不能删除流水线,可以删除分支
        // 这里不能直接查询T_PIPELINE_YAML_VERSION,因为旧流水线,可能已经存在正式版本,但是在T_PIPELINE_YAML_VERSION表没有数据
        val releaseVersionExists = pipelineYamlResourceManager.existsReleaseVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            isTemplate = GitActionCommon.isTemplateFile(filePath)
        )
        if (releaseVersionExists) {
            logger.info(
                "[PAC_PIPELINE]|release version exists, cannot be deleted|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref"
            )
            return Pair(false, true)
        }
        // 如果没有正式版本,则判断当前流水线是否只有当前分支的分支版本,如果只有当前分支版本并且当前分支没有合并请求,则可以删除
        val activeBranchList = pipelineYamlService.listRef(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        return when {
            // 只有当前分支的分支版本,则还需要判断分支是否在merge中,如果没有合并请求,则可以删除流水线,否则不操作
            activeBranchList.size == 1 && activeBranchList.contains(ref) -> {
                // TODO 判断分支是否有合并请求
                Pair(true, false)
            }
            // 除了当前分支版本,还存在其他版本,可以删除分支版本
            activeBranchList.size > 1 && activeBranchList.contains(ref) -> {
                Pair(false, true)
            }

            else ->
                Pair(false, false)
        }
    }

    /**
     * 删除源分支分支版本当合并时
     */
    private fun PipelineYamlFileEvent.deleteSourceWhenMerged(pipelineId: String) {
        val sourceRef = GitActionCommon.getSourceRef(
            fork = fork,
            sourceFullName = sourceFullName!!,
            sourceBranch = sourceBranch!!
        )
        val sourceBranchEvent = this.copy(ref = sourceRef)
        sourceBranchEvent.deleteBranchVersion(pipelineId = pipelineId)
        // fork库,合入后需要将源分支文件删除
        if (fork) {
            pipelineYamlFileService.deleteBranchFile(
                projectId = projectId,
                repoHashId = repoHashId,
                branch = sourceRef,
                filePath = filePath,
                softDelete = false
            )
        }
    }

    /**
     * 删除分支版本
     *
     */
    private fun PipelineYamlFileEvent.deleteBranchVersion(pipelineId: String) {
        logger.info(
            "[PAC_PIPELINE]|delete pipeline branch version|eventId:$eventId|" +
                    "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|" +
                    "ref:$ref|commitId:${commit?.commitId}|pipelineId:$pipelineId"
        )
        pipelineYamlService.updateBranchAction(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            branchAction = BranchVersionAction.INACTIVE.name
        )
        pipelineYamlResourceManager.updateBranchVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = ref,
            releaseBranch = true,
            branchVersionAction = BranchVersionAction.INACTIVE,
            isTemplate = GitActionCommon.isTemplateFile(filePath)
        )
        pipelineYamlService.refreshPipelineYamlStatus(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            defaultBranch = defaultBranch
        )
    }

    /**
     * 删除流水线
     */
    private fun PipelineYamlFileEvent.deletePipeline(pipelineId: String) {
        logger.info(
            "[PAC_PIPELINE]|delete pipeline|eventId:$eventId|" +
                    "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|" +
                    "ref:$ref|commitId:${commit?.commitId}|pipelineId:$pipelineId"
        )
        pipelineYamlService.deleteYamlPipeline(
            userId = authUser,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        pipelineYamlResourceManager.deletePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            isTemplate = GitActionCommon.isTemplateFile(filePath)
        )
    }
}
