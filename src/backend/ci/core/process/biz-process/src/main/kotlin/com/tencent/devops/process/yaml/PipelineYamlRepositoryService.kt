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
 *
 */

package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByPacRepo
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.webhook.PipelineWebhookVersion
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.process.yaml.git.pojo.PacGitPushResult
import com.tencent.devops.process.yaml.modelTransfer.aspect.PipelineTransferAspectLoader
import com.tencent.devops.process.yaml.pojo.PipelineYamlTriggerLock
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
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
    private val pipelineYamlSyncService: PipelineYamlSyncService
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
        logger.info("syncYamlPipeline|$projectId|pipeline:$triggerPipeline|yamlFile:$yamlFile")
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
        val pipelineYamlVersion = pipelineYamlService.getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = action.data.setting.repoHashId,
            filePath = yamlFile.yamlPath,
            blobId = yamlFile.blobId!!
        )
        val defaultBranch = action.data.context.defaultBranch
        val branch = action.data.eventCommon.branch
        // yaml版本不存在
        if (pipelineYamlVersion == null) {
            updateYamlPipeline(
                projectId = projectId,
                pipelineId = pipelineId,
                action = action,
                yamlFile = yamlFile,
                needDeleteVersion = false
            )
        } else {
            // 默认分支推送，并且版本的创建分支不是从默认分支,说明分支合入主干,需要将分支版本转换成稳定版本
            if (branch == defaultBranch && pipelineYamlVersion.ref != defaultBranch) {
                updateYamlPipeline(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    action = action,
                    yamlFile = yamlFile,
                    needDeleteVersion = true
                )
            }
        }
    }

    private fun createYamlPipeline(
        projectId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        logger.info("create yaml pipeline|$projectId|$yamlFile")
        val yamlContent = action.getYamlContent(yamlFile.yamlPath)
        val branch = action.data.eventCommon.branch
        val userId = action.data.getUserId()
        val repoHashId = action.data.setting.repoHashId
        val directory = GitActionCommon.getCiDirectory(yamlFile.yamlPath)
        val deployPipelineResult = pipelineInfoFacadeService.createYamlPipeline(
            userId = action.data.setting.enableUser,
            projectId = projectId,
            yaml = yamlContent.content,
            yamlPath = yamlFile.yamlPath,
            branchName = branch,
            isDefaultBranch = branch == action.data.context.defaultBranch,
            aspects = PipelineTransferAspectLoader.initByDefaultTriggerOn(defaultRepo = {
                action.data.setting.aliasName
            })
        )
        val pipelineId = deployPipelineResult.pipelineId
        val version = deployPipelineResult.version
        val webhooks = getWebhooks(projectId = projectId, pipelineId = pipelineId, version = version)
        pipelineYamlService.save(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = yamlFile.yamlPath,
            directory = directory,
            blobId = yamlFile.blobId!!,
            commitId = action.data.eventCommon.commit.commitId,
            ref = branch,
            pipelineId = pipelineId,
            version = version,
            userId = userId,
            webhooks = webhooks
        )
        // 流水线保存后,再创建流水线组,不然流水线组无法加载到流水线
        createYamlViewIfAbsent(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            repoHashId = repoHashId,
            gitProjectName = action.data.setting.projectName,
            directory = directory
        )
    }

    /**
     * 创建流水线组
     */
    private fun createYamlViewIfAbsent(
        userId: String,
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        gitProjectName: String,
        directory: String
    ) {
        val pipelineYamlView = pipelineYamlService.getPipelineYamlView(
            projectId = projectId,
            repoHashId = repoHashId,
            directory
        )
        // 存在则不再创建
        if (pipelineYamlView != null) {
            // 流水线先创建后保存到T_PIPELINE_YAML_INFO表,再创建流水线时刷新流水线组还加载不到yaml流水线,所以这里需要再加载一次
            pipelineViewGroupService.updateGroupAfterPipelineCreate(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId
            )
            return
        }
        val path = gitProjectName.substringAfterLast("/")
        val name = if (directory == Constansts.ciFileDirectoryName) {
            path
        } else {
            "$path-${directory.removePrefix(".ci/")}"
        }
        val pipelineView = PipelineViewForm(
            name = name,
            projected = true,
            viewType = PipelineViewType.DYNAMIC,
            logic = Logic.AND,
            filters = listOf(
                PipelineViewFilterByPacRepo(
                    condition = Condition.EQUAL,
                    repoHashId = repoHashId,
                    directory = directory
                )
            )
        )
        val viewHashId = pipelineViewGroupService.addViewGroup(
            projectId = projectId,
            userId = userId,
            pipelineView = pipelineView
        )
        pipelineYamlService.savePipelineYamlView(
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory,
            viewId = HashUtil.decodeIdToLong(viewHashId)
        )
    }

    /**
     * @param needDeleteVersion 是否需要删除旧的version
     */
    private fun updateYamlPipeline(
        projectId: String,
        pipelineId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry,
        needDeleteVersion: Boolean
    ) {
        logger.info("update yaml pipeline|$projectId|$yamlFile")
        val yamlContent = action.getYamlContent(yamlFile.yamlPath)
        val branch = action.data.eventCommon.branch
        val deployPipelineResult = pipelineInfoFacadeService.updateYamlPipeline(
            userId = action.data.setting.enableUser,
            projectId = projectId,
            pipelineId = pipelineId,
            yaml = yamlContent.content,
            yamlPath = yamlFile.yamlPath,
            branchName = branch,
            isDefaultBranch = branch == action.data.context.defaultBranch,
            aspects = PipelineTransferAspectLoader.initByDefaultTriggerOn(defaultRepo = {
                action.data.setting.aliasName
            })
        )
        val version = deployPipelineResult.version
        val repoHashId = action.data.setting.repoHashId
        val webhooks = getWebhooks(projectId = projectId, pipelineId = pipelineId, version = version)
        pipelineYamlService.update(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = yamlFile.yamlPath,
            blobId = yamlFile.blobId!!,
            commitId = action.data.eventCommon.commit.commitId,
            ref = branch,
            pipelineId = deployPipelineResult.pipelineId,
            version = deployPipelineResult.version,
            userId = action.data.getUserId(),
            webhooks = webhooks,
            needDeleteVersion = needDeleteVersion
        )
    }

    /**
     * 删除yaml文件
     *
     * 1.如果是默认分支删除,不做处理
     * 2.如果是非默认分支删除
     *   - 当前流水线有正式版本,分支版本置为删除
     *   - 当前流水线没有正式版本，分支版本变为草稿版本
     */
    fun deleteYamlPipeline(
        projectId: String,
        action: BaseAction
    ) {
        val yamlFile = action.data.context.yamlFile!!
        val filePath = yamlFile.yamlPath
        val repoHashId = action.data.setting.repoHashId
        val userId = action.data.getUserId()
        val defaultBranch = action.data.context.defaultBranch
        logger.info("deleteYamlPipeline|$userId|$projectId|$repoHashId|yamlFile:$yamlFile")
        try {
            if (yamlFile.ref.isNullOrBlank()) {
                return
            }
            val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
            // 非默认分支删除yaml,需要将分支版本置为无效
            if (pipelineYamlInfo != null && yamlFile.ref != defaultBranch) {
                pipelineInfoFacadeService.updateBranchVersion(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineYamlInfo.pipelineId,
                    branchName = yamlFile.ref,
                    branchVersionAction = BranchVersionAction.INACTIVE
                )
            }
            pipelineYamlService.deleteBranchFile(
                projectId = projectId,
                repoHashId = repoHashId,
                branch = yamlFile.ref,
                filePath = filePath
            )
        } catch (ignored: Exception) {
            logger.warn("Failed to delete pipeline yaml|$projectId|${action.format()}", ignored)
            throw ignored
        }
    }

    /**
     * 发布yaml流水线
     */
    fun releaseYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        action: BaseAction,
        gitPushResult: PacGitPushResult
    ) {
        val repoHashId = action.data.setting.repoHashId
        val filePath = gitPushResult.filePath

        val webhooks = getWebhooks(projectId = projectId, pipelineId = pipelineId, version = version)
        PipelineYamlTriggerLock(
            redisOperation = redisOperation,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        ).use {
            it.lock()
            createOrUpdateYamlPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                repoHashId = repoHashId,
                filePath = filePath,
                gitPushResult = gitPushResult,
                gitProjectName = action.data.setting.projectName,
                webhooks = webhooks
            )
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
        gitProjectName: String,
        webhooks: List<PipelineWebhookVersion>
    ) {
        val blobId = gitPushResult.blobId
        val branch = gitPushResult.branch
        val commitId = gitPushResult.lastCommitId
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        if (pipelineYamlInfo == null) {
            val directory = GitActionCommon.getCiDirectory(filePath)
            pipelineYamlService.save(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                directory = directory,
                pipelineId = pipelineId,
                userId = userId,
                blobId = blobId,
                commitId = commitId,
                ref = branch,
                version = version,
                webhooks = webhooks
            )
            createYamlViewIfAbsent(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                repoHashId = repoHashId,
                directory = directory,
                gitProjectName = gitProjectName
            )
        } else {
            val pipelineYamlVersion = pipelineYamlService.getPipelineYamlVersion(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId
            )
            if (pipelineYamlVersion == null) {
                pipelineYamlService.update(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    blobId = blobId,
                    commitId = commitId,
                    ref = branch,
                    pipelineId = pipelineId,
                    version = version,
                    userId = userId,
                    webhooks = webhooks,
                    needDeleteVersion = false
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
            pipelineInfoFacadeService.updateYamlPipelineSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineYamlInfo.pipelineId,
                pipelineAsCodeSettings = PipelineAsCodeSettings(enable = false)
            )
            pipelineYamlService.deleteYamlPipeline(
                userId = userId,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = pipelineYamlInfo.filePath
            )
        }
        // 删除流水线组
        val yamlViews = pipelineYamlService.listRepoYamlView(projectId = projectId, repoHashId = repoHashId)
        yamlViews.forEach { yamlView ->
            pipelineViewGroupService.deleteViewGroup(
                projectId = projectId,
                userId = userId,
                viewIdEncode = HashUtil.encodeLongId(yamlView.viewId),
                checkPac = false
            )
            pipelineYamlService.deleteYamlView(
                projectId = projectId,
                repoHashId = repoHashId,
                directory = yamlView.directory
            )
        }
        // 删除yaml同步记录
        pipelineYamlSyncService.delete(projectId = projectId, repoHashId = repoHashId)
    }

    /**
     * TODO 需优化
     * 本来应该在com.tencent.devops.process.engine.service.PipelineWebhookService.addWebhook处理,
     * 但是这个方法是异步的,可能有延迟，所以再保存一次,后续需要优化
     */
    private fun getWebhooks(
        projectId: String,
        pipelineId: String,
        version: Int
    ): List<PipelineWebhookVersion> {
        val model =
            pipelineRepositoryService.getModel(projectId = projectId, pipelineId = pipelineId, version = version)
        if (model == null) {
            logger.info("$pipelineId|$version|model is null")
            return emptyList()
        }
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val variables = triggerContainer.params.associate { param ->
            param.id to param.defaultValue.toString()
        }
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
