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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.pipeline.PushPipelineResult
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.webhook.WebhookEventFactory
import com.tencent.devops.process.yaml.actions.EventActionFactory
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.actions.data.PacRepoSetting
import com.tencent.devops.process.yaml.actions.data.YamlTriggerPipeline
import com.tencent.devops.process.yaml.actions.internal.event.PipelineYamlManualEvent
import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.process.yaml.mq.PipelineYamlEnableEvent
import com.tencent.devops.process.yaml.mq.PipelineYamlTriggerEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Suppress("ComplexMethod")
class PipelineYamlFacadeService @Autowired constructor(
    private val client: Client,
    private val eventActionFactory: EventActionFactory,
    private val dslContext: DSLContext,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val objectMapper: ObjectMapper,
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val webhookEventFactory: WebhookEventFactory,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val pipelineYamlService: PipelineYamlService,
    @Lazy
    private val pipelineYamlRepositoryService: PipelineYamlRepositoryService,
    private val pipelineYamlViewService: PipelineYamlViewService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlFacadeService::class.java)
    }

    fun enablePac(userId: String, projectId: String, repoHashId: String, scmType: ScmType) {
        logger.info("enable pac|$userId|$projectId|$repoHashId|$scmType")
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        ).data ?: return
        val setting = PacRepoSetting(repository = repository)
        val event = PipelineYamlManualEvent(
            userId = userId,
            projectId = projectId,
            repoHashId = repoHashId,
            scmType = scmType
        )
        val action = eventActionFactory.loadManualEvent(setting = setting, event = event)

        val yamlPathList = action.getYamlPathList()
        pipelineYamlSyncService.initPacSyncDetail(
            projectId = projectId,
            repoHashId = repoHashId,
            yamlPathList = yamlPathList
        )
        // 如果没有Yaml文件则不初始化
        if (yamlPathList.isEmpty()) {
            logger.warn("enable pac,not found ci yaml from git|$projectId|$repoHashId")
            return
        }
        // 创建yaml流水线组
        pipelineYamlViewService.createYamlViewIfAbsent(
            userId = action.data.getUserId(),
            projectId = projectId,
            repoHashId = repoHashId,
            gitProjectName = action.data.setting.projectName,
            directoryList = yamlPathList.map { GitActionCommon.getCiDirectory(it.yamlPath) }.toSet()
        )
        val path2PipelineExists = pipelineYamlInfoDao.getAllByRepo(
            dslContext = dslContext, projectId = projectId, repoHashId = repoHashId
        ).associate {
            it.filePath to YamlTriggerPipeline(
                projectId = it.projectId,
                repoHashId = it.repoHashId,
                filePath = it.filePath,
                pipelineId = it.pipelineId,
                userId = userId
            )
        }
        yamlPathList.forEach {
            action.data.context.pipeline = path2PipelineExists[it.yamlPath]
            action.data.context.yamlFile = it
            pipelineEventDispatcher.dispatch(
                PipelineYamlEnableEvent(
                    projectId = projectId,
                    yamlPath = it.yamlPath,
                    userId = userId,
                    eventStr = objectMapper.writeValueAsString(event),
                    metaData = action.metaData,
                    actionCommonData = action.data.eventCommon,
                    actionContext = action.data.context,
                    actionSetting = action.data.setting
                )
            )
        }
    }

    fun trigger(
        eventObject: CodeWebhookEvent,
        scmType: ScmType,
        requestId: String,
        eventTime: LocalDateTime
    ) {
        try {
            logger.info("pipeline yaml trigger|$requestId|$scmType")
            val action = eventActionFactory.load(eventObject)
            if (action == null) {
                logger.warn("pipeline yaml trigger|request event not support|$eventObject")
                return
            }
            // 初始化setting
            if (!action.data.isSettingInitialized) {
                val externalId = action.data.eventCommon.gitProjectId
                val repository = client.get(ServiceRepositoryPacResource::class).getPacRepository(
                    externalId = externalId, scmType = scmType
                ).data ?: run {
                    return
                }
                val setting = PacRepoSetting(repository = repository)
                action.data.setting = setting
            }

            action.initCacheData()

            val projectId = action.data.setting.projectId
            val repoHashId = action.data.setting.repoHashId
            val yamlPathList = action.getYamlPathList()
            // 如果没有Yaml文件则不初始化
            if (yamlPathList.isEmpty()) {
                logger.warn("pipeline yaml trigger not found ci yaml from git|$projectId|$repoHashId")
                return
            }

            val matcher = webhookEventFactory.createScmWebHookMatcher(scmType = scmType, event = action.data.event)
            val eventId = pipelineTriggerEventService.getEventId(
                projectId = projectId,
                requestId = requestId,
                eventSource = repoHashId
            )
            val triggerEvent = PipelineTriggerEvent(
                projectId = projectId,
                eventId = eventId,
                triggerType = scmType.name,
                eventSource = repoHashId,
                eventType = matcher.getEventType().name,
                triggerUser = matcher.getUsername(),
                eventDesc = matcher.getEventDesc(),
                requestId = requestId,
                createTime = eventTime
            )
            pipelineTriggerEventService.saveTriggerEvent(triggerEvent)
            action.data.context.eventId = eventId
            // 创建yaml流水线组
            pipelineYamlViewService.createYamlViewIfAbsent(
                userId = action.data.getUserId(),
                projectId = projectId,
                repoHashId = repoHashId,
                gitProjectName = action.data.setting.projectName,
                directoryList = yamlPathList.map { GitActionCommon.getCiDirectory(it.yamlPath) }.toSet()
            )
            val path2PipelineExists = pipelineYamlInfoDao.getAllByRepo(
                dslContext = dslContext, projectId = projectId, repoHashId = repoHashId
            ).associate {
                it.filePath to YamlTriggerPipeline(
                    projectId = it.projectId,
                    repoHashId = it.repoHashId,
                    filePath = it.filePath,
                    pipelineId = it.pipelineId,
                    userId = it.creator
                )
            }
            val eventStr = if (action.metaData.streamObjectKind == StreamObjectKind.REVIEW) {
                objectMapper.writeValueAsString(
                    (action.data.event as GitReviewEvent).copy(
                        objectKind = GitReviewEvent.classType
                    )
                )
            } else {
                objectMapper.writeValueAsString(action.data.event as GitEvent)
            }
            yamlPathList.forEach {
                action.data.context.pipeline = path2PipelineExists[it.yamlPath]
                action.data.context.yamlFile = it
                action.data.context.eventId = eventId
                pipelineEventDispatcher.dispatch(
                    PipelineYamlTriggerEvent(
                        projectId = projectId,
                        yamlPath = it.yamlPath,
                        userId = action.data.getUserId(),
                        eventStr = eventStr,
                        metaData = action.metaData,
                        actionCommonData = action.data.eventCommon,
                        actionContext = action.data.context,
                        actionSetting = action.data.setting,
                        scmType = scmType
                    )
                )
            }
        } catch (ignored: Throwable) {
            logger.warn("pipeline yaml trigger", ignored)
        }
    }

    fun disablePac(userId: String, projectId: String, repoHashId: String, scmType: ScmType) {
        logger.info("disable pac|$userId|$projectId|$repoHashId|$scmType")
        pipelineYamlRepositoryService.disablePac(
            userId = userId,
            projectId = projectId,
            repoHashId = repoHashId
        )
    }

    fun getPipelineYamlInfo(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineYamlVo? {
        return pipelineYamlService.getPipelineYamlVo(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    fun yamlExistInDefaultBranch(
        projectId: String,
        pipelineId: String
    ): Boolean {
        return pipelineYamlService.yamlExistInDefaultBranch(
            projectId = projectId,
            pipelineIds = listOf(pipelineId)
        )[pipelineId] ?: false
    }

    fun pushYamlFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        versionName: String?,
        pipelineName: String,
        repoHashId: String,
        scmType: ScmType,
        filePath: String,
        content: String,
        commitMessage: String,
        targetAction: CodeTargetAction
    ): PushPipelineResult {
        logger.info("push yaml file|$userId|$projectId|$pipelineId|$repoHashId|$scmType|$version|$versionName")
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.GIT_NOT_FOUND,
            params = arrayOf(repoHashId)
        )
        checkPushParam(content, repoHashId, filePath, targetAction, versionName, projectId, pipelineId)
        try {
            val setting = PacRepoSetting(repository = repository)
            val event = PipelineYamlManualEvent(
                userId = userId,
                projectId = projectId,
                repoHashId = repoHashId,
                scmType = scmType
            )
            val action = eventActionFactory.loadManualEvent(setting = setting, event = event)
            // 发布时创建流水线
            pipelineYamlViewService.createYamlViewIfAbsent(
                userId = action.data.getUserId(),
                projectId = projectId,
                repoHashId = repoHashId,
                gitProjectName = action.data.setting.projectName,
                directoryList = setOf(GitActionCommon.getCiDirectory(filePath))
            )
            val gitPushResult = pipelineYamlRepositoryService.releaseYamlPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                version = version,
                versionName = versionName,
                action = action,
                filePath = filePath,
                content = content,
                commitMessage = commitMessage,
                targetAction = targetAction
            )
            return PushPipelineResult(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = gitPushResult.filePath,
                branch = gitPushResult.branch,
                mrUrl = gitPushResult.mrUrl
            )
        } catch (exception: Exception) {
            logger.error("Failed to push yaml file|$userId|$projectId|$pipelineId|$repoHashId")
            throw exception
        }
    }

    private fun checkPushParam(
        content: String,
        repoHashId: String,
        filePath: String,
        targetAction: CodeTargetAction,
        versionName: String?,
        projectId: String,
        pipelineId: String
    ) {
        if (content.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_YAML_CONTENT_IS_EMPTY,
                params = arrayOf(repoHashId)
            )
        }
        if (filePath.startsWith(Constansts.ciFileDirectoryName) &&
            !GitActionCommon.checkYamlPipelineFile(filePath.substringAfter(".ci/"))
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_YAML_FILE_NAME_FORMAT
            )
        }
        if (targetAction != CodeTargetAction.COMMIT_TO_MASTER && versionName.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("versionName")
            )
        }
        pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId, repoHashId = repoHashId, filePath = filePath
        )?.let {
            if (it.pipelineId != pipelineId) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_YAML_BOUND_PIPELINE,
                    params = arrayOf(filePath, it.pipelineId)
                )
            }
        }
        pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
            if (it.repoHashId != repoHashId) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BOUND_REPO,
                    params = arrayOf(it.repoHashId)
                )
            }
            if (it.filePath != filePath) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BOUND_YAML,
                    params = arrayOf(it.filePath)
                )
            }
        }
    }

    /**
     * 构建yaml流水线触发变量
     */
    fun buildYamlManualParamMap(userId: String, projectId: String, pipelineId: String): Map<String, BuildParameters>? {
        val pipelineYamlInfo = pipelineYamlInfoDao.get(
            dslContext = dslContext, projectId = projectId, pipelineId = pipelineId
        ) ?: return null
        val repoHashId = pipelineYamlInfo.repoHashId
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        ).data ?: return null
        val setting = PacRepoSetting(repository = repository)
        val event = PipelineYamlManualEvent(
            userId = userId,
            projectId = projectId,
            repoHashId = repoHashId,
            scmType = repository.getScmType()
        )
        val action = eventActionFactory.loadManualEvent(setting = setting, event = event)
        return mutableMapOf(
            BK_REPO_WEBHOOK_HASH_ID to BuildParameters(BK_REPO_WEBHOOK_HASH_ID, repoHashId),
            PIPELINE_WEBHOOK_BRANCH to BuildParameters(
                PIPELINE_WEBHOOK_BRANCH, action.data.context.defaultBranch ?: ""
            )
        )
    }
}
