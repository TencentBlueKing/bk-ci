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

package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PAC_DEFAULT_BRANCH_FILE_DELETED
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_REF_YAML_FILE_NOT_FOUND
import com.tencent.devops.process.plugin.trigger.service.PipelineYamlBranchTimerService
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlInfo
import com.tencent.devops.process.pojo.pipeline.enums.PipelineYamlStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.trigger.scm.listener.PipelineYamlChangeContext
import com.tencent.devops.process.trigger.scm.listener.PipelineYamlChangeManager
import com.tencent.devops.process.yaml.common.YamlFileUtils
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.process.yaml.pojo.PipelineYamlTriggerLock
import com.tencent.devops.process.yaml.pojo.YamlPipelineActionType
import com.tencent.devops.process.yaml.resource.PipelineYamlResourceManager
import com.tencent.devops.process.yaml.resource.PipelineYamlViewResourceClient
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 单文件事件驱动的 YAML 生命周期变更。
 *
 * PAC 生命周期编排入口见 [PipelineYamlPacManager]。
 */
@Service
class PipelineYamlFileManager @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineYamlViewResourceClient: PipelineYamlViewResourceClient,
    private val pipelineYamlChangeManager: PipelineYamlChangeManager,
    private val pipelineYamlFileService: PipelineYamlFileService,
    private val pipelineYamlResourceManager: PipelineYamlResourceManager,
    private val pipelineYamlBranchTimerService: PipelineYamlBranchTimerService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlFileManager::class.java)
    }

    fun createOrUpdateYamlFile(event: PipelineYamlFileEvent): Boolean {
        with(event) {
            val lock = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
            return try {
                lock.lock()
                doCreateOrUpdateYamlFile()
            } finally {
                lock.unlock()
            }
        }
    }

    /**
     * 创建或更新yaml流水线,调用方必须已持有[filePath]对应的文件锁,锁不可重入,重复加锁会等待到锁过期
     */
    private fun PipelineYamlFileEvent.doCreateOrUpdateYamlFile(): Boolean {
        logger.info(
            "[PAC_PIPELINE]|create or update yaml pipeline|$eventId|" +
                "$projectId|$repoHashId|$filePath|$ref|${commit?.commitId}|$blobId"
        )
        val context = PipelineYamlChangeContext(
            projectId = projectId,
            filePath = filePath,
            eventId = eventId,
            actionType = YamlPipelineActionType.CREATE
        )
        return try {
            checkParam()
            createOrUpdateYamlPipeline(context = context)
            pipelineYamlChangeManager.fireChangeSuccess(context = context)
            true
        } catch (ignored: Exception) {
            logger.error(
                "[PAC_PIPELINE]|Failed to create or update yaml pipeline|$eventId|" +
                        "$projectId$repoHashId|$filePath|$ref|${commit?.commitId}|$blobId",
                ignored
            )
            handlePullRequestOnFailed(context = context, exception = ignored)
            pipelineYamlChangeManager.fireChangeError(context = context, exception = ignored)
            false
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
                "[PAC_PIPELINE]|delete pipeline yaml|$eventId|$projectId|$repoHashId|$filePath|$ref"
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
                pipelineYamlChangeManager.fireChangeSuccess(context = context)
                true
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to delete pipeline yaml|$eventId|" +
                            "$projectId|$repoHashId|$filePath|$ref",
                    ignored
                )
                pipelineYamlChangeManager.fireChangeError(context = context, exception = ignored)
                false
            } finally {
                lock.unlock()
            }
        }
    }

    fun renameYamlFile(event: PipelineYamlFileEvent): Boolean {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|rename pipeline yaml|$eventId|$projectId|$repoHashId|$filePath|$oldFilePath|$ref"
            )
            if (oldFilePath.isNullOrBlank()) {
                logger.error("old file path cannot be empty")
                return false
            }

            // 按字典序排序后依次加锁，保证所有线程加锁顺序一致，避免死锁
            val paths = listOf(oldFilePath!!, filePath).sorted()
            val lock1 = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = paths[0]
            )
            val lock2 = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = paths[1]
            )
            val context = PipelineYamlChangeContext(
                projectId = projectId,
                filePath = filePath,
                oldFilePath = oldFilePath,
                eventId = eventId,
                actionType = YamlPipelineActionType.RENAME
            )
            return try {
                lock1.lock()
                lock2.lock()
                checkParam()
                val success = renameYamlPipeline(context = context)
                if (success) {
                    pipelineYamlChangeManager.fireChangeSuccess(
                        context = context
                    )
                }
                success
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to rename yaml" +
                        "|$eventId|$projectId|$repoHashId|$oldFilePath->$filePath|$ref",
                    ignored
                )
                handlePullRequestOnFailed(context = context, exception = ignored)
                pipelineYamlChangeManager.fireChangeError(
                    context = context, exception = ignored
                )
                false
            } finally {
                lock2.unlock()
                lock1.unlock()
            }
        }
    }

    /**
     * yaml 文件无内容变更时的处理。
     * 例如 MR 合入后未出现在变更列表中的文件,仍需同步合并请求状态。
     */
    fun triggerYamlFile(event: PipelineYamlFileEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|trigger pipeline yaml|$eventId|$projectId|$repoHashId|$filePath"
            )
            val notMergedPullRequest = pullRequestId == null || pullRequestUrl == null ||
                pullRequestNumber == null || !merged
            if (notMergedPullRequest) {
                return
            }
            val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                includeOldFilePath = true
            ) ?: run {
                logger.info("[PAC_PIPELINE]|yaml pipeline not found|$projectId|$repoHashId|$filePath")
                return
            }
            // pr 合并后,通知资源更新状态
            pipelineYamlResourceManager.completePullRequest(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineYamlInfo.pipelineId,
                pullRequestId = pullRequestId!!,
                pullRequestUrl = pullRequestUrl!!,
                pullRequestNumber = pullRequestNumber!!,
                merged = true,
                isTemplate = isTemplate
            )
        }
    }

    /**
     * 合并请求关闭操作
     */
    fun closeYamlFile(event: PipelineYamlFileEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|close pipeline yaml|$eventId|$projectId|$repoHashId|$filePath"
            )
            if (pullRequestId == null || pullRequestUrl == null || pullRequestNumber == null) {
                logger.info(
                    "[PAC_PIPELINE]|close yaml file|pull request is null|" +
                            "$eventId|$projectId|$repoHashId|$filePath|$ref"
                )
                return
            }
            val context = PipelineYamlChangeContext(
                projectId = projectId,
                filePath = filePath,
                eventId = eventId,
                actionType = YamlPipelineActionType.CLOSE
            )
            val lock = PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
            try {
                lock.lock()
                val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    includeOldFilePath = true
                ) ?: run {
                    logger.info("[PAC_PIPELINE]|yaml pipeline not found|$projectId|$repoHashId|$filePath")
                    return
                }
                val pipelineId = pipelineYamlInfo.pipelineId
                val pipelineName = pipelineYamlResourceManager.getPipelineName(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    isTemplate = isTemplate
                ) ?: run {
                    throw ErrorCodeException(
                        errorCode = ERROR_PIPELINE_NOT_EXISTS,
                        params = arrayOf(pipelineId)
                    )
                }
                context.pipelineId = pipelineId
                context.versionName = pipelineName
                pipelineYamlResourceManager.completePullRequest(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineYamlInfo.pipelineId,
                    pullRequestId = pullRequestId ?: 0L,
                    pullRequestUrl = pullRequestUrl ?: "",
                    pullRequestNumber = pullRequestNumber ?: 0,
                    merged = merged,
                    isTemplate = isTemplate
                )
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to close pipeline yaml|$eventId|$projectId|$repoHashId|$filePath",
                    ignored
                )
                pipelineYamlChangeManager.fireChangeError(context = context, exception = ignored)
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineYamlFileEvent.checkParam() {
        val missingParam = when {
            commit == null -> "commit"
            authRepository == null -> "authRepository"
            blobId == null -> "blobId"
            else -> return
        }
        logger.error(
            "[PAC_PIPELINE]|$missingParam cannot be empty|$eventId|$projectId|$repoHashId|$filePath|$ref"
        )
        throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf(missingParam)
        )
    }

    private fun PipelineYamlFileEvent.createOrUpdateYamlPipeline(context: PipelineYamlChangeContext) {
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        if (pipelineYamlInfo == null) {
            checkConflictWithDefaultBranch()
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
                isTemplate = isTemplate
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
            context.pipelineName = pipelineName

            updatePipelineIfAbsent(pipelineId = pipelineId)?.let {
                context.versionName = it.versionName
                // 如果yaml更新了流水线名称,日志也需要更新
                context.pipelineName = it.pipelineName
            } ?: run {
                context.actionType = YamlPipelineActionType.NO_CHANGE
            }
            handlePullRequestOnSuccess(pipelineId = pipelineId)
        }
    }

    private fun PipelineYamlFileEvent.createYamlPipeline(): DeployPipelineResult {
        val directory = YamlFileUtils.getCiDirectory(filePath)
        val content = pipelineYamlFileService.getFileContent(
            projectId = projectId,
            path = filePath,
            ref = commit!!.commitId,
            authRepository = authRepository!!
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_REF_TEMPLATE_YAML_FILE_NOT_FOUND,
            params = arrayOf(filePath, commit!!.commitId)
        )
        val resourceType = YamlFileUtils.getYamlResourceType(
            filePath = filePath,
            fileContent = content.content
        )
        val deployPipelineResult = pipelineYamlResourceManager.createYamlPipeline(
            userId = authUser,
            projectId = projectId,
            yaml = content.content,
            event = this
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
            commitId = commit?.commitId ?: "",
            commitTime = commit?.commitTime ?: LocalDateTime.now(),
            pipelineId = pipelineId,
            status = if (ref == defaultBranch) {
                PipelineYamlStatus.OK.name
            } else {
                PipelineYamlStatus.UN_MERGED.name
            },
            version = version,
            userId = userId,
            resourceType = resourceType
        )
        pipelineYamlViewResourceClient.updateGroupAfterPipelineUpdate(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = deployPipelineResult.pipelineName,
            creator = userId,
            userId = userId
        )
        logger.info(
            "[PAC_PIPELINE]|create pipeline|$eventId|" +
                    "$projectId|$repoHashId|$filePath|$ref|$pipelineId|$version|${deployPipelineResult.versionName}"
        )
        reconcileYamlBranchTimer(deployPipelineResult)
        return deployPipelineResult
    }

    private fun PipelineYamlFileEvent.updatePipelineIfAbsent(pipelineId: String): DeployPipelineResult? {
        val needCreateVersion = shouldCreateVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            commitId = commit?.commitId ?: "",
            blobId = blobId ?: "",
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
                    "$projectId|$repoHashId|$filePath|$commitId|${pipelineYamlVersion.version}"
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
                        "$projectId|$repoHashId|$filePath|$blobId|${it.version}"
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
                        "$projectId|$repoHashId|$filePath|$blobId|${pipelineYamlVersion.version}"
                )
                false
            } else {
                true
            }
        }
    }

    private fun PipelineYamlFileEvent.updateYamlPipeline(
        pipelineId: String
    ): DeployPipelineResult {
        val content = pipelineYamlFileService.getFileContent(
            projectId = projectId,
            path = filePath,
            ref = commit!!.commitId,
            authRepository = authRepository!!
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_PIPELINE_REF_YAML_FILE_NOT_FOUND,
            params = arrayOf(filePath, commit!!.commitId)
        )
        val deployPipelineResult = pipelineYamlResourceManager.updateYamlPipeline(
            userId = authUser,
            projectId = projectId,
            pipelineId = pipelineId,
            yaml = content.content,
            event = this
        )
        val resourceType = YamlFileUtils.getYamlResourceType(
            filePath = filePath,
            fileContent = content.content
        )
        pipelineYamlService.update(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            blobId = content.blobId,
            commitId = commit?.commitId ?: "",
            commitTime = commit?.commitTime ?: LocalDateTime.now(),
            ref = ref,
            defaultBranch = defaultBranch,
            pipelineId = deployPipelineResult.pipelineId,
            version = deployPipelineResult.version,
            userId = userId,
            resourceType = resourceType
        )
        reconcileYamlBranchTimer(deployPipelineResult)
        logger.info(
            "[PAC_PIPELINE]|update pipeline version|$eventId|" +
                "$projectId|$repoHashId|$filePath|$ref|" +
                "${deployPipelineResult.version}|${deployPipelineResult.versionName}"
        )
        return deployPipelineResult
    }

    private fun PipelineYamlFileEvent.deletePipelineOrBranchVersion(context: PipelineYamlChangeContext) {
        logger.info(
            "[PAC_PIPELINE]|delete pipeline or branch version|$eventId|" +
                "$projectId|$repoHashId|$filePath|$ref"
        )
        // 先删除分支文件信息
        pipelineYamlFileService.deleteBranchFile(
            projectId = projectId,
            repoHashId = repoHashId,
            branch = ref,
            filePath = filePath,
            softDelete = ref == defaultBranch
        )
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            includeOldFilePath = true
        ) ?: run {
            logger.info("[PAC_PIPELINE]|yaml pipeline not found|$projectId|$repoHashId|$filePath")
            context.actionType = YamlPipelineActionType.NO_CHANGE
            return
        }
        val pipelineId = pipelineYamlInfo.pipelineId
        val pipelineName = pipelineYamlResourceManager.getPipelineName(
            projectId = projectId,
            pipelineId = pipelineId,
            isTemplate = isTemplate
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
            deleteYamlPipeline(pipelineId = pipelineId)
        } else {
            if (shouldDeleteVersion) {
                context.actionType = YamlPipelineActionType.DELETE_VERSION
                deleteBranchVersion(pipelineId = pipelineId)
            } else {
                context.actionType = YamlPipelineActionType.NO_CHANGE
                logger.info(
                    "[PAC_PIPELINE]|branch version has deleted|$eventId|$projectId|$repoHashId|$filePath|$ref"
                )
            }
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
            isTemplate = isTemplate
        )
        if (releaseVersionExists) {
            logger.info(
                "[PAC_PIPELINE]|release version exists, cannot be deleted|$eventId|" +
                    "$projectId|$repoHashId|$filePath|$ref"
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
     * 合并到目标分支后处理源分支版本与 pr 状态
     */
    private fun PipelineYamlFileEvent.handlePullRequestOnSuccess(pipelineId: String) {
        // 如果合并到目标分支或者 fork 仓库合并,需要将源分支的分支版本删除
        if (!merged || (ref != defaultBranch && !fork)) {
            return
        }
        deleteSourceWhenMerged(pipelineId = pipelineId)
        // pr 合并后,通知资源更新状态
        pipelineYamlResourceManager.completePullRequest(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pullRequestId = pullRequestId!!,
            pullRequestUrl = pullRequestUrl!!,
            pullRequestNumber = pullRequestNumber!!,
            merged = true,
            isTemplate = isTemplate
        )
    }

    /**
     * MR 已合并但 yaml 处理失败时的兜底,避免模板实例状态一直停留在"更新中"
     */
    private fun PipelineYamlFileEvent.handlePullRequestOnFailed(
        context: PipelineYamlChangeContext,
        exception: Exception
    ) {
        val pipelineId = context.pipelineId
        // 仅处理"MR 已合并 + 有合并请求 + 已知 pipelineId"的场景
        if (!merged || pullRequestId == null || pipelineId.isNullOrBlank()) {
            return
        }
        runCatching {
            pipelineYamlResourceManager.completePullRequest(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pullRequestId = pullRequestId!!,
                pullRequestUrl = pullRequestUrl ?: "",
                pullRequestNumber = pullRequestNumber ?: 0,
                merged = merged,
                isTemplate = isTemplate,
                exception = exception
            )
        }.onFailure {
            logger.warn(
                "[PAC_PIPELINE]|complete pull request on failed error|$projectId|$pipelineId",
                it
            )
        }
    }

    /**
     * 校验创建流水线前默认分支文件状态
     */
    private fun PipelineYamlFileEvent.checkConflictWithDefaultBranch() {
        // 如果不是默认分支,需要判断默认分支是否已经删除,如果默认分支已删除,更新不能再创建新的流水线
        // 这种情况是,当默认分支已经删除,其他分支还对该文件进行修改
        if (ref == defaultBranch) {
            return
        }
        val defaultBranchFileDeleted = pipelineYamlFileService.getBranchFilePath(
            projectId = projectId,
            repoHashId = repoHashId,
            branch = defaultBranch,
            filePath = filePath
        )?.deleted ?: false
        if (defaultBranchFileDeleted && actionType == YamlFileActionType.UPDATE) {
            throw ErrorCodeException(
                errorCode = ERROR_PAC_DEFAULT_BRANCH_FILE_DELETED,
                params = arrayOf(filePath)
            )
        }
    }

    /**
     * 删除源分支分支版本当合并时
     */
    private fun PipelineYamlFileEvent.deleteSourceWhenMerged(pipelineId: String) {
        val sourceRef = YamlFileUtils.getSourceRef(
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
            "[PAC_PIPELINE]|delete pipeline branch version|$eventId|" +
                "$projectId|$repoHashId|$filePath|$ref|${commit?.commitId}|$pipelineId"
        )
        if (!isTemplate) {
            pipelineYamlBranchTimerService.deleteBranch(
                userId = authUser,
                projectId = projectId,
                pipelineId = pipelineId,
                branch = ref
            )
        }
        pipelineYamlResourceManager.updateBranchAction(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = ref,
            branchVersionAction = BranchVersionAction.INACTIVE,
            isTemplate = isTemplate
        )
        pipelineYamlService.updateBranchAction(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            branchAction = BranchVersionAction.INACTIVE.name
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
    private fun PipelineYamlFileEvent.deleteYamlPipeline(pipelineId: String) {
        logger.info(
            "[PAC_PIPELINE]|delete pipeline|$eventId|" +
                "$projectId|$repoHashId|$filePath|$ref|${commit?.commitId}|$pipelineId"
        )
        // 先删除yaml关联关系,再删除流水线,这样如果后面失败,用户可以页面删除
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
            isTemplate = isTemplate
        )
        // 删除流水线后清理流水线组
        if (!isTemplate) {
            val directory = YamlFileUtils.getCiDirectory(filePath)
            pipelineYamlViewResourceClient.cleanupYamlView(
                userId = userId,
                projectId = projectId,
                repoHashId = repoHashId,
                directory = directory,
                pipelineId = pipelineId
            )
        }
    }

    /**
     * 重命名 YAML 流水线,流水线ID不变
     *
     * 1. 默认分支,
     *     - 如果新文件对应的INFO不存在,则创建,如果已经存在,则判断是否需要创建新的版本
     *     - 删除老文件的INFO
     * 2. 非默认分支
     *     - 如果新文件对应的INFO不存在,则创建,如果已经存在,则判断是否需要创建新的版本
     *     - 不删除老文件的INFO
     *     - 删除旧文件创建的分支版本
     */
    private fun PipelineYamlFileEvent.renameYamlPipeline(
        context: PipelineYamlChangeContext
    ): Boolean {
        val oldYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = oldFilePath!!
        )

        val newYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        if (oldYamlInfo == null && newYamlInfo == null) {
            logger.info(
                "[PAC_PIPELINE]|rename yaml pipeline not found|" +
                    "$eventId|$projectId|$repoHashId|$filePath|$oldFilePath|$ref"
            )
            // 变更事件已由doCreateOrUpdateYamlFile自行上报,重置为NO_CHANGE避免重复记录重命名结果
            context.actionType = YamlPipelineActionType.NO_CHANGE
            // 重命名已持有新旧文件路径的锁,这里不能再走加锁的createOrUpdateYamlFile
            return doCreateOrUpdateYamlFile()
        }
        if (newYamlInfo != null && oldYamlInfo != null &&
            newYamlInfo.pipelineId != oldYamlInfo.pipelineId
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PAC_YAML_FILE_BINDTO_OTHER_PIPELINE,
                params = arrayOf(filePath, newYamlInfo.pipelineId)
            )
        }
        val pipelineId = newYamlInfo?.pipelineId ?: oldYamlInfo!!.pipelineId
        val pipelineName = pipelineYamlResourceManager.getPipelineName(
            projectId = projectId,
            pipelineId = pipelineId,
            isTemplate = isTemplate
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        context.pipelineName = pipelineName
        context.pipelineId = pipelineId

        // 校验是否需要删除info记录,需要在deleteOldBranchVersion之前判断
        val needDeleteOldInfo = shouldDeleteOldYamlInfo(oldFilePath = oldFilePath!!)
        deleteOldBranchVersion(pipelineId = pipelineId)
        updateYamlPipelineWhenRename(
            pipelineId = pipelineId,
            newYamlInfo = newYamlInfo
        )?.let {
            context.versionName = it.versionName
            context.pipelineName = it.pipelineName
        } ?: run {
            context.actionType = YamlPipelineActionType.NO_CHANGE
        }
        deleteOldYamlPipeline(
            pipelineId = pipelineId,
            needDeleteOldInfo = needDeleteOldInfo
        )
        handlePullRequestOnSuccess(pipelineId = pipelineId)
        return true
    }

    /**
     * 删除旧文件创建的分支版本
     *
     * 重命名,需要先删除旧文件创建的分支版本,不然等新文件创建后,就无法判断是新文件创建的还是旧文件创建的分支版本,
     */
    private fun PipelineYamlFileEvent.deleteOldBranchVersion(pipelineId: String) {
        if (ref == defaultBranch) {
            return
        }
        val oldPipelineYamlVersion = pipelineYamlService.getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = oldFilePath!!,
            ref = ref,
            branchAction = BranchVersionAction.ACTIVE.name
        )
        if (oldPipelineYamlVersion != null) {
            val oldFileEvent = this.copy(
                filePath = oldFilePath!!,
                oldFilePath = null
            )
            oldFileEvent.deleteBranchVersion(pipelineId = pipelineId)
        }
    }

    private fun PipelineYamlFileEvent.updateYamlPipelineWhenRename(
        pipelineId: String,
        newYamlInfo: PipelineYamlInfo?
    ): DeployPipelineResult? {
        val commitId = commit!!.commitId
        // 新文件是否需要创建新版本
        val needCreateVersion = newYamlInfo == null || shouldCreateVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            commitId = commitId,
            blobId = blobId!!,
            defaultBranch = defaultBranch
        )
        if (!needCreateVersion) {
            logger.info(
                "[PAC_PIPELINE]|rename yaml pipeline not need create version|" +
                    "$eventId|$projectId|$repoHashId|$filePath|$oldFilePath|$ref|$commitId|$blobId"
            )
            return null
        }
        val content = pipelineYamlFileService.getFileContent(
            projectId = projectId,
            path = filePath,
            ref = commitId,
            authRepository = authRepository!!
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_REF_TEMPLATE_YAML_FILE_NOT_FOUND,
            params = arrayOf(filePath, commitId)
        )
        val deployPipelineResult = pipelineYamlResourceManager.updateYamlPipeline(
            userId = authUser,
            projectId = projectId,
            pipelineId = pipelineId,
            yaml = content.content,
            event = this
        )
        val resourceType = YamlFileUtils.getYamlResourceType(
            filePath = filePath,
            fileContent = content.content
        )
        val directory = YamlFileUtils.getCiDirectory(filePath)
        pipelineYamlService.rename(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            oldFilePath = oldFilePath!!,
            directory = directory,
            defaultBranch = defaultBranch,
            pipelineId = deployPipelineResult.pipelineId,
            status = if (ref == defaultBranch) {
                PipelineYamlStatus.OK.name
            } else {
                PipelineYamlStatus.UN_MERGED.name
            },
            userId = userId,
            resourceType = resourceType,
            blobId = content.blobId,
            commitId = commit!!.commitId,
            commitTime = commit!!.commitTime,
            ref = ref,
            version = deployPipelineResult.version,
            needCreateNewInfo = newYamlInfo == null
        )
        // 重命名后,可能修改文件路径,需要更新流水线组
        pipelineYamlViewResourceClient.updateGroupAfterPipelineUpdate(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = deployPipelineResult.pipelineName,
            creator = userId,
            userId = userId
        )
        reconcileYamlBranchTimer(deployPipelineResult)
        return deployPipelineResult
    }

    private fun PipelineYamlFileEvent.reconcileYamlBranchTimer(result: DeployPipelineResult) {
        if (isTemplate) {
            return
        }
        yamlBranchScheduleNotes = pipelineYamlBranchTimerService.reconcile(
            userId = authUser,
            projectId = projectId,
            pipelineId = result.pipelineId,
            version = result.version,
            branch = ref,
            defaultBranch = defaultBranch,
            repoHashId = repoHashId,
            filePath = filePath,
            channelCode = ChannelCode.BS
        )
    }

    private fun PipelineYamlFileEvent.deleteOldYamlPipeline(
        pipelineId: String,
        needDeleteOldInfo: Boolean
    ) {
        logger.info(
            "[PAC_PIPELINE]|delete old yaml pipeline|" +
                "$eventId|$projectId|$repoHashId|$filePath|$oldFilePath|$needDeleteOldInfo"
        )
        pipelineYamlService.deleteOldFile(
            projectId = projectId,
            repoHashId = repoHashId,
            ref = ref,
            defaultBranch = defaultBranch,
            oldFilePath = oldFilePath!!,
            needDeleteOldInfo = needDeleteOldInfo
        )
        // 重命名导致目录变更时，从旧目录流水线组移除并清理空组；同目录重命名无需处理
        if (!isTemplate && needDeleteOldInfo) {
            val oldDirectory = YamlFileUtils.getCiDirectory(oldFilePath!!)
            val newDirectory = YamlFileUtils.getCiDirectory(filePath)
            if (oldDirectory != newDirectory) {
                pipelineYamlViewResourceClient.cleanupYamlView(
                    userId = userId,
                    projectId = projectId,
                    repoHashId = repoHashId,
                    directory = oldDirectory,
                    pipelineId = pipelineId
                )
            }
        }
    }

    /**
     * 判定 rename 时是否应该删除旧 filePath 的 T_PIPELINE_YAML_INFO 记录。
     *
     * - 默认分支 rename：直接删除（主干视角下旧文件名已不存在）
     * - 非默认分支 rename：
     *   - 默认分支存在 → 保留
     *   - 默认分支不存在但 listRef 结果只包含当前 ref → 删除
     *   - 默认分支不存在且 listRef 还有其他 ref → 保留
     */
    private fun PipelineYamlFileEvent.shouldDeleteOldYamlInfo(
        oldFilePath: String
    ): Boolean {
        if (ref == defaultBranch) {
            return true
        }
        val defaultBranchFile = pipelineYamlFileService.getBranchFilePath(
            projectId = projectId,
            repoHashId = repoHashId,
            branch = defaultBranch,
            filePath = oldFilePath
        )
        // 默认分支是否存在旧文件,存在则不删除info信息
        if (defaultBranchFile != null && !defaultBranchFile.deleted) {
            logger.info(
                "[PAC_PIPELINE]|default branch file exists, keep old yaml info|" +
                    "$eventId|$projectId|$repoHashId|$oldFilePath|$ref"
            )
            return false
        }
        val activeBranchList = pipelineYamlService.listRef(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = oldFilePath
        )
        return activeBranchList.size == 1 && activeBranchList.contains(ref)
    }
}
