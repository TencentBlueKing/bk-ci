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
 *
 */

package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.PIPELINE_PAC_REPO_HASH_ID
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.pipeline.PipelineYamlView
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.pipeline.enums.PipelineYamlStatus
import com.tencent.devops.process.pojo.webhook.PipelineWebhookVersion
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.actions.internal.PipelineYamlManualAction
import com.tencent.devops.process.yaml.git.pojo.PacGitPushResult
import com.tencent.devops.process.yaml.pojo.PipelineYamlTriggerLock
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectLoader
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlRepositoryService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val pipelineYamlViewService: PipelineYamlViewService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlRepositoryService::class.java)
    }

    fun deployYamlPipeline(
        projectId: String,
        action: BaseAction
    ) {
        val triggerPipeline = action.data.context.pipeline
        val yamlFile = action.data.context.yamlFile!!
        val filePath = yamlFile.yamlPath
        val commitId = action.data.eventCommon.commit.commitId
        logger.info("deployYamlPipeline|$projectId|pipeline:$triggerPipeline|yamlFile:$yamlFile|commitId:$commitId")
        try {
            PipelineYamlTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = action.data.setting.repoHashId,
                filePath = filePath
            ).use {
                it.lock()
                if (triggerPipeline == null) {
                    createPipelineIfAbsent(
                        projectId = projectId,
                        action = action,
                        yamlFile = yamlFile
                    )
                } else {
                    updatePipelineIfAbsent(
                        projectId = projectId,
                        pipelineId = triggerPipeline.pipelineId,
                        action = action,
                        yamlFile = yamlFile
                    )
                }
            }
        } catch (ignored: Exception) {
            logger.warn(
                "Failed to deploy pipeline yaml|$projectId|${action.data.setting.repoHashId}|yamlFile:$yamlFile",
                ignored
            )
            throw ignored
        }
    }

    /**
     * 创建yaml流水线,如果流水线不存在则创建，存在则更新版本
     */
    private fun createPipelineIfAbsent(
        projectId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        // 再次确认yaml文件是否已经创建出流水线
        pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = action.data.setting.repoHashId,
            filePath = yamlFile.yamlPath
        )?.let {
            updatePipelineIfAbsent(
                projectId = projectId,
                pipelineId = it.pipelineId,
                action = action,
                yamlFile = yamlFile
            )
        } ?: run {
            createYamlPipeline(
                projectId = projectId,
                action = action,
                yamlFile = yamlFile
            )
        }
    }

    private fun updatePipelineIfAbsent(
        projectId: String,
        pipelineId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        val branch = action.data.eventCommon.branch
        val defaultBranch = action.data.context.defaultBranch
        val needCreateVersion = needCreateVersion(
            projectId = projectId,
            repoHashId = action.data.setting.repoHashId,
            filePath = yamlFile.yamlPath,
            ref = GitActionCommon.getRealRef(action = action, branch = branch),
            blobId = yamlFile.blobId!!,
            defaultBranch = defaultBranch!!
        )
        if (needCreateVersion) {
            updateYamlPipeline(
                projectId = projectId,
                pipelineId = pipelineId,
                action = action,
                yamlFile = yamlFile
            )
        }
    }

    /**
     * 判断是否需要创建流水线新版本
     *
     * 1. 当前分支没有分支版本
     *  - 文件blobId在默认分支存在版本,说明分支合并了默认分支,则不创建分支版本
     *  - 文件blobId在默认分支不存在版本,说明是在新分支更新的文件,则创建分支版本
     * 2. 当前分支有分支版本
     *  - 分支版本最新blobId与文件的blobId不一样,则需要创建分支版本
     */
    private fun needCreateVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        blobId: String,
        defaultBranch: String
    ): Boolean {
        val pipelineYamlVersion = pipelineYamlService.getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            branchAction = BranchVersionAction.ACTIVE.name
        )
        return if (pipelineYamlVersion == null) {
            if (defaultBranch != ref) {
                pipelineYamlService.getPipelineYamlVersion(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    ref = defaultBranch,
                    blobId = blobId,
                    branchAction = BranchVersionAction.ACTIVE.name
                ) == null
            } else {
                true
            }
        } else {
            pipelineYamlVersion.blobId != blobId
        }
    }

    private fun createYamlPipeline(
        projectId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        logger.info("create yaml pipeline|$projectId|$yamlFile")
        val yamlContent = action.getYamlContent(yamlFile.yamlPath)
        val fork = action.data.eventCommon.fork
        val branch = action.data.eventCommon.branch
        val userId = action.data.getUserId()
        val repoHashId = action.data.setting.repoHashId
        val directory = GitActionCommon.getCiDirectory(yamlFile.yamlPath)
        val defaultBranch = action.data.context.defaultBranch
        // 不是fork仓库，并且分支等于默认分支
        val isDefaultBranch = !fork && branch == defaultBranch
        val commitId = action.data.eventCommon.commit.commitId
        val commitTime = action.data.eventCommon.commit.commitTimeStamp?.let {
            DateTimeUtil.stringToLocalDateTime(it)
        } ?: LocalDateTime.now()
        val ref = GitActionCommon.getRealRef(action = action, branch = branch)
        val yamlInfo = PipelineYamlVo(
            repoHashId = repoHashId,
            filePath = yamlFile.yamlPath
        )
        val deployPipelineResult = pipelineInfoFacadeService.createYamlPipeline(
            userId = action.data.setting.enableUser,
            projectId = projectId,
            yaml = yamlContent.content,
            yamlFileName = yamlFile.yamlPath.removePrefix(".ci/"),
            branchName = ref,
            isDefaultBranch = isDefaultBranch,
            description = action.data.eventCommon.commit.commitMsg,
            aspects = PipelineTransferAspectLoader.initByDefaultTriggerOn(defaultRepo = {
                action.data.setting.aliasName
            }),
            yamlInfo = yamlInfo
        )
        val pipelineId = deployPipelineResult.pipelineId
        val version = deployPipelineResult.version
        val webhooks = getWebhooks(
            projectId = projectId, pipelineId = pipelineId, version = version, repoHashId = repoHashId
        )
        pipelineYamlService.save(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = yamlFile.yamlPath,
            directory = directory,
            defaultBranch = defaultBranch,
            blobId = yamlFile.blobId!!,
            ref = ref,
            commitId = commitId,
            commitTime = commitTime,
            pipelineId = pipelineId,
            status = if (isDefaultBranch) {
                PipelineYamlStatus.OK.name
            } else {
                PipelineYamlStatus.UN_MERGED.name
            },
            version = version,
            userId = userId,
            webhooks = webhooks
        )
        pipelineViewGroupService.updateGroupAfterPipelineUpdate(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = deployPipelineResult.pipelineName,
            creator = userId,
            userId = userId
        )
    }

    private fun updateYamlPipeline(
        projectId: String,
        pipelineId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        logger.info("update yaml pipeline|$projectId|$yamlFile")
        val yamlContent = action.getYamlContent(yamlFile.yamlPath)
        val fork = action.data.eventCommon.fork
        val branch = action.data.eventCommon.branch
        val defaultBranch = action.data.context.defaultBranch
        // 不是fork仓库，并且分支等于默认分支
        val isDefaultBranch = !fork && branch == defaultBranch
        val commitId = action.data.eventCommon.commit.commitId
        val commitTime = action.data.eventCommon.commit.commitTimeStamp?.let {
            DateTimeUtil.stringToLocalDateTime(it)
        } ?: LocalDateTime.now()
        // 如果是fork仓库,ref应该加上fork库的namespace
        val ref = GitActionCommon.getRealRef(action = action, branch = branch)
        val repoHashId = action.data.setting.repoHashId

        val yamlInfo = PipelineYamlVo(
            repoHashId = repoHashId,
            filePath = yamlFile.yamlPath
        )
        val deployPipelineResult = pipelineInfoFacadeService.updateYamlPipeline(
            userId = action.data.setting.enableUser,
            projectId = projectId,
            pipelineId = pipelineId,
            yaml = yamlContent.content,
            yamlFileName = yamlFile.yamlPath.removePrefix(".ci/"),
            branchName = ref,
            isDefaultBranch = isDefaultBranch,
            description = action.data.eventCommon.commit.commitMsg,
            aspects = PipelineTransferAspectLoader.initByDefaultTriggerOn(defaultRepo = {
                action.data.setting.aliasName
            }),
            yamlInfo = yamlInfo
        )
        val version = deployPipelineResult.version

        val webhooks = getWebhooks(
            projectId = projectId, pipelineId = pipelineId, version = version, repoHashId = repoHashId
        )
        pipelineYamlService.update(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = yamlFile.yamlPath,
            blobId = yamlFile.blobId!!,
            commitId = commitId,
            commitTime = commitTime,
            ref = ref,
            defaultBranch = defaultBranch,
            pipelineId = deployPipelineResult.pipelineId,
            version = deployPipelineResult.version,
            userId = action.data.getUserId(),
            webhooks = webhooks
        )
    }

    /**
     * 删除yaml文件
     *
     * 1.如果是默认分支删除,不做处理
     * 2.如果是非默认分支删除
     *   - 当前流水线有正式版本,分支版本置为删除
     *   - 当前流水线没有正式版本，分支版本变为草稿版本
     *
     *  @param releaseBranch true-merge分支,false-删除分支或删除文件
     */
    fun deleteYamlPipeline(
        projectId: String,
        action: BaseAction,
        releaseBranch: Boolean? = false
    ) {
        val yamlFile = action.data.context.yamlFile!!
        val filePath = yamlFile.yamlPath
        val repoHashId = action.data.setting.repoHashId
        val userId = action.data.getUserId()
        val defaultBranch = action.data.context.defaultBranch
        val ref = yamlFile.ref
        logger.info("deleteYamlPipeline|$userId|$projectId|$repoHashId|yamlFile:$yamlFile")
        if (ref.isNullOrBlank()) {
            return
        }
        val lock = PipelineYamlTriggerLock(
            redisOperation = redisOperation,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        try {
            lock.lock()
            pipelineYamlService.deleteBranchFile(
                projectId = projectId,
                repoHashId = repoHashId,
                branch = ref,
                filePath = filePath
            )
            val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            ) ?: return
            val needUnbindYamlPipeline = needUnbindYamlPipeline(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                defaultBranch = defaultBranch
            )
            // 只有删除分支或者删除文件时才能解绑
            if (releaseBranch == false && needUnbindYamlPipeline) {
                unBindYamlPipeline(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineYamlInfo.pipelineId,
                    repoHashId = repoHashId,
                    filePath = yamlFile.yamlPath,
                    refreshView = true
                )
            } else {
                pipelineYamlService.updateBranchAction(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = yamlFile.yamlPath,
                    ref = ref,
                    branchAction = BranchVersionAction.INACTIVE.name
                )
                pipelineInfoFacadeService.updateBranchVersion(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineYamlInfo.pipelineId,
                    branchName = yamlFile.ref,
                    releaseBranch = releaseBranch,
                    branchVersionAction = BranchVersionAction.INACTIVE
                )
                if (defaultBranch != null) {
                    pipelineYamlService.refreshPipelineYamlStatus(
                        projectId = projectId,
                        repoHashId = repoHashId,
                        filePath = yamlFile.yamlPath,
                        defaultBranch = defaultBranch
                    )
                }
            }
        } catch (ignored: Exception) {
            logger.warn("Failed to delete pipeline yaml|$projectId|${action.format()}", ignored)
            throw ignored
        } finally {
            lock.unlock()
        }
    }

    /**
     * 是否需要解绑PAC流水线
     * 1. 默认分支yaml删除,直接解绑
     * 2. 非默认分支删除
     *      - 默认分支存在yaml文件,不能解绑
     *      - 默认分支不存在yaml文件,当前流水线最新版本对应的分支与当前分支相同,可以解绑
     */
    private fun needUnbindYamlPipeline(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        defaultBranch: String?
    ): Boolean {
        return when {
            defaultBranch == null -> false
            ref == defaultBranch -> true
            else -> {
                // 如果存在稳定版本,说明yaml在默认分支存在,那么在非默认分支删除,不能关闭PAC,
                // 否则判断当前流水线最新版本是不是当前分支创建,是-删除,不是-不删
                pipelineYamlService.getPipelineYamlVersion(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    ref = defaultBranch
                )?.let { false } ?: run {
                    pipelineYamlService.getPipelineYamlVersion(
                        projectId = projectId,
                        repoHashId = repoHashId,
                        filePath = filePath
                    )?.ref == ref
                }
            }
        }
    }

    /**
     * 发布yaml流水线
     */
    fun releaseYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        version: Int,
        versionName: String?,
        action: PipelineYamlManualAction,
        filePath: String,
        content: String,
        commitMessage: String,
        targetAction: CodeTargetAction,
        targetBranch: String?
    ): PacGitPushResult {
        val repoHashId = action.data.setting.repoHashId
        val webhooks = getWebhooks(
            projectId = projectId, pipelineId = pipelineId, version = version, repoHashId = repoHashId
        )
        PipelineYamlTriggerLock(
            redisOperation = redisOperation,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            expiredTimeInSeconds = 180
        ).use {
            it.lock()
            // 推送到工蜂必须在锁内，不然流水线版本会不一致
            val gitPushResult = action.pushYamlFile(
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                filePath = filePath,
                content = content,
                commitMessage = commitMessage,
                targetAction = targetAction,
                versionName = versionName,
                targetBranch = targetBranch
            )
            createOrUpdateYamlPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                repoHashId = repoHashId,
                filePath = filePath,
                gitPushResult = gitPushResult,
                action = action,
                webhooks = webhooks
            )
            return gitPushResult
        }
    }

    private fun createOrUpdateYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        repoHashId: String,
        filePath: String,
        gitPushResult: PacGitPushResult,
        action: BaseAction,
        webhooks: List<PipelineWebhookVersion>
    ) {
        val blobId = gitPushResult.blobId
        val branch = gitPushResult.branch
        val commitId = gitPushResult.commitId
        val commitTime = DateTimeUtil.stringToLocalDateTime(gitPushResult.commitTime)
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        val defaultBranch = action.data.context.defaultBranch
        val isDefaultBranch = gitPushResult.branch == defaultBranch
        if (pipelineYamlInfo == null) {
            logger.info("push yaml pipeline|create yaml|$projectId|$pipelineId|$version")
            val directory = GitActionCommon.getCiDirectory(filePath)
            pipelineYamlService.save(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                directory = directory,
                defaultBranch = defaultBranch,
                pipelineId = pipelineId,
                status = if (isDefaultBranch) {
                    PipelineYamlStatus.OK.name
                } else {
                    PipelineYamlStatus.UN_MERGED.name
                },
                userId = userId,
                ref = branch,
                commitId = commitId,
                commitTime = commitTime,
                blobId = blobId,
                version = version,
                webhooks = webhooks
            )
            pipelineViewGroupService.updateGroupAfterPipelineCreate(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId
            )
        } else {
            val needCreateVersion = needCreateVersion(
                projectId = projectId,
                repoHashId = action.data.setting.repoHashId,
                filePath = filePath,
                ref = branch,
                blobId = blobId,
                defaultBranch = defaultBranch!!
            )
            if (needCreateVersion) {
                logger.info("push yaml pipeline|update yaml|$projectId|$pipelineId|$version")
                pipelineYamlService.update(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    ref = branch,
                    commitId = commitId,
                    commitTime = commitTime,
                    blobId = blobId,
                    defaultBranch = defaultBranch,
                    pipelineId = pipelineId,
                    version = version,
                    userId = userId,
                    webhooks = webhooks
                )
            }
        }
    }

    fun disablePac(
        userId: String,
        projectId: String,
        repoHashId: String
    ) {
        val yamlPipelines = pipelineYamlService.getAllYamlPipeline(projectId = projectId, repoHashId = repoHashId)
        yamlPipelines.forEach { pipelineYamlInfo ->
            // 解绑yaml关联的流水线
            unBindYamlPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineYamlInfo.pipelineId,
                repoHashId = repoHashId,
                filePath = pipelineYamlInfo.filePath,
                refreshView = false
            )
        }
        // 删除流水线组
        val yamlViews = pipelineYamlViewService.listRepoYamlView(projectId = projectId, repoHashId = repoHashId)
        yamlViews.forEach { yamlView ->
            deleteYamlView(
                projectId = projectId,
                userId = userId,
                yamlView = yamlView
            )
        }
        // 删除yaml同步记录
        pipelineYamlSyncService.delete(projectId = projectId, repoHashId = repoHashId)
    }

    /**
     * 解绑流水线PAC,有两种情况会解绑流水线PAC
     * 1. 代码库直接关闭PAC，关闭前需要删除所有默认分支yaml文件
     * 2. 默认分支删除yaml文件
     * 3. 删除非默认分支yaml文件，并且流水线没有稳定版本
     *
     * @param refreshView 是否刷新流水线组
     */
    private fun unBindYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        filePath: String,
        refreshView: Boolean
    ) {
        // 解绑yaml关联的流水线
        pipelineInfoFacadeService.updateYamlPipelineSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineAsCodeSettings = PipelineAsCodeSettings(enable = false)
        )
        pipelineYamlService.deleteYamlPipeline(
            userId = userId,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        if (refreshView) {
            // 如果PAC流水线组已经没有流水线了,那么就将这个流水线组删除
            val directory = GitActionCommon.getCiDirectory(filePath)
            val yamlPipelineCnt = pipelineYamlService.countPipelineYaml(
                projectId = projectId,
                repoHashId = repoHashId,
                directory = directory
            )
            if (yamlPipelineCnt == 0L) {
                logger.info("delete pipeline yaml view|$projectId|$repoHashId|$directory")
                pipelineYamlViewService.getPipelineYamlView(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    directory = directory
                )?.let {
                    deleteYamlView(
                        projectId = projectId,
                        userId = userId,
                        yamlView = it
                    )
                }
            }
            val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
            pipelineViewGroupService.updateGroupAfterPipelineUpdate(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineInfo.pipelineName,
                creator = userId,
                userId = userId
            )
        }
    }

    private fun deleteYamlView(
        projectId: String,
        userId: String,
        yamlView: PipelineYamlView
    ) {
        pipelineViewGroupService.deleteViewGroup(
            projectId = projectId,
            userId = userId,
            viewIdEncode = HashUtil.encodeLongId(yamlView.viewId),
            checkPac = false
        )
        pipelineYamlViewService.deleteYamlView(
            projectId = projectId,
            repoHashId = yamlView.repoHashId,
            directory = yamlView.directory
        )
    }

    /**
     * TODO 需优化
     * 本来应该在com.tencent.devops.process.engine.service.PipelineWebhookService.addWebhook处理,
     * 但是这个方法是异步的,可能有延迟，所以再保存一次,后续需要优化
     */
    private fun getWebhooks(
        projectId: String,
        pipelineId: String,
        version: Int,
        repoHashId: String
    ): List<PipelineWebhookVersion> {
        val model = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        )?.model
        if (model == null) {
            logger.info("$pipelineId|$version|model is null")
            return emptyList()
        }
        val triggerContainer = model.getTriggerContainer()
        val variables = PipelineVarUtil.fillVariableMap(
            triggerContainer.params.associate { param ->
                param.id to param.defaultValue.toString()
            }
        ).toMutableMap()
        // 补充yaml流水线代码库信息
        variables[PIPELINE_PAC_REPO_HASH_ID] = repoHashId

        val elements = triggerContainer.elements.filterIsInstance<WebHookTriggerElement>()
        val webhooks = mutableListOf<PipelineWebhookVersion>()
        elements.forEach { element ->
            try {
                val (scmType, eventType, repositoryConfig) =
                    RepositoryConfigUtils.buildWebhookConfig(element, variables)
                val repository = pipelineWebhookService.registerWebhook(
                    projectId = projectId,
                    scmType = scmType,
                    repositoryConfig = repositoryConfig,
                    codeEventType = eventType,
                    elementVersion = element.version
                ) ?: return@forEach
                webhooks.add(
                    PipelineWebhookVersion(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        version = version,
                        taskId = element.id!!,
                        taskParams = JsonUtil.toJson(element.genTaskParams()),
                        taskRepoType = repositoryConfig.repositoryType,
                        taskRepoHashId = repositoryConfig.repositoryHashId,
                        taskRepoName = repositoryConfig.repositoryName,
                        repositoryType = scmType,
                        repositoryHashId = repository.repoHashId!!,
                        eventType = eventType!!.name
                    )
                )
            } catch (ignore: Exception) {
                logger.warn("$projectId|$pipelineId|add webhook failed", ignore)
            }
        }
        return webhooks
    }
}
