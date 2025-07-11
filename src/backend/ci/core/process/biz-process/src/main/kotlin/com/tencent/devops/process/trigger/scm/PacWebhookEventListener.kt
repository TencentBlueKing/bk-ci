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

package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.trigger.scm.converter.WebhookConverterManager
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.process.yaml.PipelineYamlViewService
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.process.yaml.pojo.YamlFileActionType.CREATE
import com.tencent.devops.process.yaml.pojo.YamlFileActionType.RENAME
import com.tencent.devops.process.yaml.pojo.YamlFileActionType.UPDATE
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * PAC流水线webhook事件监听者
 */
@Service
class PacWebhookEventListener(
    private val pipelineYamlViewService: PipelineYamlViewService,
    private val pipelineYamlService: PipelineYamlService,
    private val eventDispatcher: SampleEventDispatcher,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val webhookConverterManager: WebhookConverterManager,
    private val webhookGrayService: WebhookGrayService
) : WebHookEventListener {

    companion object {
        private val logger = LoggerFactory.getLogger(PacWebhookEventListener::class.java)
    }

    override fun onEvent(eventId: Long, repository: Repository, webhook: Webhook, replayPipelineId: String?) {
        if (!webhookGrayService.isPacGrayRepo(repository.scmCode, repository.projectName)) {
            return
        }
        if (repository.enablePac != true) return
        val projectId = repository.projectId!!
        val context = WebhookTriggerContext(
            projectId = projectId,
            pipelineId = Constansts.ciFileDirectoryName,
            eventId = eventId
        )
        try {
            val yamlFileEvents = webhookConverterManager.convert(
                eventId = eventId,
                repository = repository,
                webhook = webhook
            )
            val filterYamlFileEvents = filterReplayYamlFile(
                projectId = projectId,
                repository = repository,
                yamlFileEvents = yamlFileEvents,
                replayPipelineId = replayPipelineId
            )
            createPipelineView(
                userId = repository.userName,
                projectId = projectId,
                repository = repository,
                yamlFileEvents = filterYamlFileEvents
            )
            filterYamlFileEvents.forEach {
                eventDispatcher.dispatch(it)
            }
        } catch (ignored: Exception) {
            logger.error(
                "[PAC_PIPELINE]|Failed to dispatch yaml file event|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:${repository.repoHashId}|" +
                        "eventType:${webhook.eventType}",
                ignored
            )
            webhookTriggerManager.fireError(context = context, exception = ignored)
        }
    }

    /**
     * 过滤单条重试的流水线
     */
    private fun filterReplayYamlFile(
        projectId: String,
        repository: Repository,
        yamlFileEvents: List<PipelineYamlFileEvent>,
        replayPipelineId: String?
    ): List<PipelineYamlFileEvent> {
        return if (replayPipelineId != null) {
            val pipelineYamlInfo = if (PipelineUtils.isPipelineId(replayPipelineId)) {
                pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = replayPipelineId)
            } else {
                pipelineYamlService.getPipelineYamlInfo(
                    projectId = projectId,
                    repoHashId = repository.repoHashId!!,
                    filePath = replayPipelineId
                )
            } ?: run {
                logger.warn("replay pipeline not enable pac|$projectId|$replayPipelineId")
                return emptyList()
            }
            yamlFileEvents.filter { it.filePath == pipelineYamlInfo.filePath }
        } else {
            yamlFileEvents
        }
    }

    private fun createPipelineView(
        projectId: String,
        userId: String,
        repository: Repository,
        yamlFileEvents: List<PipelineYamlFileEvent>
    ) {
        val directories = yamlFileEvents.filter {
            // 创建、更新、重命名事件
            setOf(CREATE, UPDATE, RENAME).contains(it.actionType)
        }.map { GitActionCommon.getCiDirectory(it.filePath) }.toSet()
        if (directories.isNotEmpty()) {
            // 创建yaml流水线组
            pipelineYamlViewService.createYamlViewIfAbsent(
                userId = userId,
                projectId = projectId,
                repoHashId = repository.repoHashId!!,
                aliasName = repository.aliasName,
                directoryList = directories
            )
        }
    }
}
