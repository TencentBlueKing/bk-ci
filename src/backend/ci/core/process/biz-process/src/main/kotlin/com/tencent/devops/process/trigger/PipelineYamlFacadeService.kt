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

package com.tencent.devops.process.trigger

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.trigger.actions.EventActionFactory
import com.tencent.devops.process.trigger.actions.data.PacRepoSetting
import com.tencent.devops.process.trigger.actions.data.PacTriggerPipeline
import com.tencent.devops.process.trigger.actions.pacActions.data.PacEnableEvent
import com.tencent.devops.process.trigger.actions.pacActions.data.PacPushYamlFileEvent
import com.tencent.devops.process.trigger.mq.pacTrigger.PacYamlEnableEvent
import com.tencent.devops.process.trigger.mq.pacTrigger.PacYamlTriggerEvent
import com.tencent.devops.process.webhook.WebhookEventFactory
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineYamlFacadeService @Autowired constructor(
    private val client: Client,
    private val eventActionFactory: EventActionFactory,
    private val dslContext: DSLContext,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val traceEventDispatcher: TraceEventDispatcher,
    private val objectMapper: ObjectMapper,
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val webhookEventFactory: WebhookEventFactory,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val pipelineYamlResourceService: PipelineYamlResourceService
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
        val event = PacEnableEvent(
            userId = userId,
            projectId = projectId,
            repoHashId = repoHashId,
            scmType = scmType
        )
        val action = eventActionFactory.loadEnableEvent(setting = setting, event = event)

        val ciDirId = action.getCiDirId()
        val yamlPathList = action.getYamlPathList()
        pipelineYamlSyncService.initPacSyncDetail(
            projectId = projectId,
            repoHashId = repoHashId,
            ciDirId = ciDirId!!,
            yamlPathList = yamlPathList
        )
        // 如果没有Yaml文件则不初始化
        if (yamlPathList.isEmpty()) {
            logger.warn("enable pac,not found ci yaml from git|$projectId|$repoHashId")
            return
        }
        val path2PipelineExists = pipelineYamlInfoDao.getAllByRepo(
            dslContext = dslContext, projectId = projectId, repoHashId = repoHashId
        ).associate {
            it.filePath to PacTriggerPipeline(
                projectId = it.projectId,
                repoHashId = it.repoHashId,
                filePath = it.filePath,
                pipelineId = it.pipelineId,
                userId = userId
            )
        }
        yamlPathList.forEach {
            val triggerPipeline = path2PipelineExists[it.yamlPath] ?: PacTriggerPipeline(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = it.yamlPath,
                pipelineId = "",
                userId = userId
            )
            action.data.context.pipeline = triggerPipeline
            action.data.context.yamlFile = it
            action.data.context.ciDirId = ciDirId
            traceEventDispatcher.dispatch(
                PacYamlEnableEvent(
                    projectId = projectId,
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
        hookRequestId: Long,
        eventTime: LocalDateTime
    ) {
        logger.info("pac yaml trigger|$hookRequestId|$scmType")
        val action = eventActionFactory.load(eventObject)
        if (action == null) {
            logger.warn("pac trigger|request event not support|$eventObject")
            return
        }
        val externalId = action.data.eventCommon.gitProjectId
        val repository = client.get(ServiceRepositoryPacResource::class).getPacRepository(
            externalId = externalId, scmType = scmType
        ).data ?: run {
            logger.info("pac yaml trigger|repository not enable pac|$externalId|$scmType")
            return
        }
        val setting = PacRepoSetting(repository = repository)
        action.data.setting = setting
        action.initCacheData()

        val projectId = repository.projectId!!
        val repoHashId = repository.repoHashId!!
        val yamlPathList = action.getYamlPathList()
        // 如果没有Yaml文件则不初始化
        if (yamlPathList.isEmpty()) {
            logger.warn("pac yaml trigger not found ci yaml from git|$projectId|$repoHashId")
            return
        }
        val matcher = webhookEventFactory.createScmWebHookMatcher(scmType = scmType, event = action.data.event)
        val eventId = pipelineTriggerEventService.getEventId()
        val triggerEvent = PipelineTriggerEvent(
            projectId = projectId,
            eventId = eventId,
            triggerType = scmType.name,
            eventSource = repoHashId,
            eventType = matcher.getEventType().name,
            triggerUser = matcher.getUsername(),
            eventDesc = matcher.getEventDesc(),
            hookRequestId = hookRequestId,
            eventTime = eventTime
        )
        pipelineTriggerEventService.saveTriggerEvent(triggerEvent)
        val path2PipelineExists = pipelineYamlInfoDao.getAllByRepo(
            dslContext = dslContext, projectId = projectId, repoHashId = repoHashId
        ).associate {
            it.filePath to PacTriggerPipeline(
                projectId = it.projectId,
                repoHashId = it.repoHashId,
                filePath = it.filePath,
                pipelineId = it.pipelineId,
                userId = it.creator
            )
        }
        yamlPathList.forEach {
            val triggerPipeline = path2PipelineExists[it.yamlPath] ?: PacTriggerPipeline(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = it.yamlPath,
                pipelineId = "",
                userId = action.data.getUserId()
            )
            action.data.context.pipeline = triggerPipeline
            action.data.context.yamlFile = it
            action.data.context.eventId = eventId
            traceEventDispatcher.dispatch(
                PacYamlTriggerEvent(
                    projectId = projectId,
                    eventStr = objectMapper.writeValueAsString(eventObject),
                    metaData = action.metaData,
                    actionCommonData = action.data.eventCommon,
                    actionContext = action.data.context,
                    actionSetting = action.data.setting,
                    scmType = scmType
                )
            )
        }
    }

    fun disablePac(userId: String, projectId: String, repoHashId: String, scmType: ScmType) {
        logger.info("disable pac|$userId|$projectId|$repoHashId|$scmType")
    }

    fun pushYamlFile(
        userId: String,
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        scmType: ScmType,
        filePath: String,
        content: String,
        commitMessage: String,
        targetAction: CodeTargetAction
    ) {
        logger.info("upload yaml file|$userId|$projectId|$repoHashId|$scmType")
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        ).data ?: return
        val setting = PacRepoSetting(repository = repository)
        val event = PacPushYamlFileEvent(
            userId = userId,
            projectId = projectId,
            repoHashId = repoHashId,
            scmType = scmType
        )
        val action = eventActionFactory.loadPushYamlFileEvent(setting = setting, event = event)
        action.pushYamlFile(
            pipelineId = pipelineId,
            filePath = filePath,
            content = content,
            commitMessage = commitMessage,
            targetAction = targetAction
        )
    }

    fun getPipelineYamlInfo(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineYamlVo? {
        return pipelineYamlResourceService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }
}
