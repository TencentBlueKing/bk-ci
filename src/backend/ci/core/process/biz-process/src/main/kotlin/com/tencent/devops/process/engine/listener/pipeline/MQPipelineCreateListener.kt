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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.listener.pipeline

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitGenericWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.constant.WebsocketCode
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.process.engine.control.CallBackControl
import com.tencent.devops.process.engine.pojo.PipelineWebhook
import com.tencent.devops.process.engine.pojo.event.PipelineCreateEvent
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.engine.utils.RepositoryUtils
import com.tencent.devops.process.websocket.page.EditPageBuild
import com.tencent.devops.process.websocket.push.WebHookWebsocketPush
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

/**
 *  MQ实现的流水线创建事件
 *
 * @version 1.0
 */
@Component
class MQPipelineCreateListener @Autowired constructor(
    private val pipelineWebhookService: PipelineWebhookService,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val redisOperation: RedisOperation,
    private val callBackControl: CallBackControl,
    private val objectMapper: ObjectMapper,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseListener<PipelineCreateEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineCreateEvent) {
        val watch = StopWatch("pipelineCreateEventWatch")
        val pipelineId = event.pipelineId
        if (event.source == ("create_pipeline")) {
            watch.start("createPipeline[pipelineId:$pipelineId]")
            callBackControl.pipelineCreateEvent(projectId = event.projectId, pipelineId = pipelineId)
            watch.stop()
        }
        if (event.source == "createWebhook") {
            logger.info("[$pipelineId] createGitWebhook!MQ内调用")
            watch.start("createWebHook[pipelineId:$pipelineId]")
            addWebHook(event.element!!, event)
            watch.stop()
        }
        logger.info("pipelineId:$pipelineId pipelineCreateEvent watch is:$watch")
    }

    private fun addWebHook(e: Element, event: PipelineCreateEvent) {
        val (repositoryConfig, scmType, eventType) = when (e) {
            is CodeGitWebHookTriggerElement -> Triple(
                    RepositoryConfigUtils.buildConfig(e),
                    ScmType.CODE_GIT,
                    e.eventType
            )
            is CodeGitlabWebHookTriggerElement -> Triple(
                    RepositoryConfigUtils.buildConfig(e),
                    ScmType.CODE_GITLAB,
                    null
            )
            is CodeSVNWebHookTriggerElement -> Triple(RepositoryConfigUtils.buildConfig(e), ScmType.CODE_SVN, null)
            is CodeGithubWebHookTriggerElement -> Triple(RepositoryConfigUtils.buildConfig(e), ScmType.GITHUB, null)
            is CodeTGitWebHookTriggerElement -> Triple(
                RepositoryConfigUtils.buildConfig(e),
                ScmType.CODE_TGIT,
                e.data.input.eventType
            )
            is CodeGitGenericWebHookTriggerElement -> {
                val repositoryConfig = if (event.variables != null) {
                    RepositoryConfigUtils.replaceCodeProp(
                        repositoryConfig = RepositoryConfigUtils.buildConfig(e),
                        variables = event.variables as Map<String, String>
                    )
                } else {
                    RepositoryConfigUtils.buildConfig(e)
                }
                Triple(
                    repositoryConfig,
                    ScmType.valueOf(e.data.input.scmType),
                    CodeEventType.valueOf(e.data.input.eventType)
                )
            }
            else -> Triple(null, null, null)
        }

        if (repositoryConfig != null && scmType != null) {
            logger.info("[${event.pipelineId}]| Trying to add the $scmType web hook for repo($repositoryConfig)")
            try {
                if (e is CodeGitGenericWebHookTriggerElement) {
                    val repo = RepositoryUtils.buildRepository(
                        projectId = event.projectId,
                        userName = event.userId,
                        scmType = scmType,
                        repositoryUrl = repositoryConfig.repositoryName!!,
                        credentialId = e.data.input.credentialId
                    )
                    pipelineWebhookService.saveWebhook(
                        pipelineWebhook = PipelineWebhook(
                            projectId = event.projectId,
                            pipelineId = event.pipelineId,
                            repositoryType = scmType,
                            repoType = repositoryConfig.repositoryType,
                            repoHashId = repositoryConfig.repositoryHashId,
                            repoName = repo.aliasName,
                            projectName = repo.projectName,
                            taskId = e.id
                        ),
                        repo = repo,
                        codeEventType = eventType,
                        hookUrl = e.data.input.hookUrl
                    )
                } else {
                    pipelineWebhookService.saveWebhook(
                        pipelineWebhook = PipelineWebhook(
                            projectId = event.projectId,
                            pipelineId = event.pipelineId,
                            repositoryType = scmType,
                            repoType = repositoryConfig.repositoryType,
                            repoHashId = repositoryConfig.repositoryHashId,
                            repoName = repositoryConfig.repositoryName,
                            taskId = e.id
                        ), codeEventType = eventType, variables = event.variables as Map<String, String>,
                        // TODO 此处需做成传入参数
                        createPipelineFlag = true
                    )
                }
            } catch (e: Exception) {
                val post = NotifyPost(
                        module = "process",
                        message = e.message!!,
                        level = NotityLevel.HIGH_LEVEL.getLevel(),
                        dealUrl = EditPageBuild().buildPage(
                                BuildPageInfo(
                                        projectId = event.projectId,
                                        pipelineId = event.pipelineId,
                                        buildId = null,
                                        atomId = null
                                )
                        ),
                        code = WebsocketCode.WEBHOOK_ADD_ERROR,
                        webSocketType = WebSocketType.changWebType(WebSocketType.WEBHOOK),
                        page = null
                )
                websocketDispatch(post, event)
                logger.error("[${event.pipelineId}]异步调用webhook返回未知异常。webSocket推送异常信息[$post]", e)
            }
        }
    }

    private fun websocketDispatch(notifyPost: NotifyPost, event: PipelineCreateEvent) {
        webSocketDispatcher.dispatch(
                WebHookWebsocketPush(
                        buildId = null,
                        pipelineId = event.pipelineId,
                        projectId = event.projectId,
                        userId = event.userId,
                        pushType = WebSocketType.WEBHOOK,
                        redisOperation = redisOperation,
                        page = notifyPost.dealUrl,
                        objectMapper = objectMapper,
                        notifyPost = notifyPost
                )
        )
    }
}
