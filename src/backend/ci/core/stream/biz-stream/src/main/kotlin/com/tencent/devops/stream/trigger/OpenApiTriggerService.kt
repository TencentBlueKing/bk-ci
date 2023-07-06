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

package com.tencent.devops.stream.trigger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.StreamOpenApiAction
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualEvent
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("LongParameterList", "ThrowsCount")
class OpenApiTriggerService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val actionFactory: EventActionFactory,
    private val streamGitConfig: StreamGitConfig,
    streamEventService: StreamEventService,
    streamBasicSettingService: StreamBasicSettingService,
    streamYamlTrigger: StreamYamlTrigger,
    streamBasicSettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    gitPipelineResourceDao: GitPipelineResourceDao,
    gitRequestEventBuildDao: GitRequestEventBuildDao,
    streamYamlBuild: StreamYamlBuild
) : BaseManualTriggerService(
    client = client,
    dslContext = dslContext,
    streamGitConfig = streamGitConfig,
    streamEventService = streamEventService,
    streamBasicSettingService = streamBasicSettingService,
    streamYamlTrigger = streamYamlTrigger,
    streamBasicSettingDao = streamBasicSettingDao,
    gitPipelineResourceDao = gitPipelineResourceDao,
    gitRequestEventBuildDao = gitRequestEventBuildDao,
    streamYamlBuild = streamYamlBuild
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OpenApiTriggerService::class.java)
    }

    override fun loadAction(
        streamTriggerSetting: StreamTriggerSetting,
        userId: String,
        triggerBuildReq: TriggerBuildReq
    ): BaseAction {
        return if (!triggerBuildReq.payload.isNullOrBlank()) {
            loadPayloadOpenApiAction(
                streamTriggerSetting = streamTriggerSetting,
                triggerBuildReq = triggerBuildReq
            )
        } else {
            loadManualOpenApiAction(
                streamTriggerSetting = streamTriggerSetting,
                userId = userId,
                triggerBuildReq = triggerBuildReq
            )
        }
    }

    override fun getStartParams(action: BaseAction, triggerBuildReq: TriggerBuildReq): Map<String, String> {
        return if (!triggerBuildReq.payload.isNullOrBlank()) {
            (action as StreamOpenApiAction).getStartParams(
                scmType = streamGitConfig.getScmType()
            )
        } else {
            emptyMap()
        }
    }

    override fun getInputParams(action: BaseAction, triggerBuildReq: TriggerBuildReq): Map<String, String>? {
        return triggerBuildReq.inputs
    }

    private fun loadPayloadOpenApiAction(
        streamTriggerSetting: StreamTriggerSetting,
        triggerBuildReq: TriggerBuildReq
    ): StreamOpenApiAction {

        val event = mockWebhookTrigger(triggerBuildReq)
        val action = StreamOpenApiAction(
            actionFactory.load(event) ?: throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "can not load action"
            ),
            triggerBuildReq.checkPipelineTrigger
        )

        // 仅支持当前仓库下的 event
        if (action.data.getGitProjectId() != GitCommonUtils.getGitProjectId(triggerBuildReq.projectId).toString()) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "Only events in the current repository [${triggerBuildReq.projectId}] are supported"
            )
        }

        val request =
            action.buildRequestEvent(triggerBuildReq.payload!!)
                ?.copy(objectKind = StreamGitObjectKind.OBJECT_KIND_OPENAPI)
                ?: throw CustomException(
                    status = Response.Status.BAD_REQUEST,
                    message = "event invalid"
                )
        val id = gitRequestEventDao.saveGitRequest(dslContext, request)
        action.data.context.requestEventId = id

        action.data.setting = streamTriggerSetting

        return action
    }

    private fun mockWebhookTrigger(triggerBuildReq: TriggerBuildReq): CodeWebhookEvent {
        // 这里使用eventType做判断，防止有些事件无法直接通过mapper得到，例如stream 的review
        if (triggerBuildReq.eventType.isNullOrBlank()) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "eventType can't be empty"
            )
        }
        val eventStr = triggerBuildReq.payload!!

        return when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> try {
                objectMapper.readValue<GitEvent>(eventStr)
            } catch (ignore: Exception) {
                logger.warn(
                    "OpenApiTriggerService|mockWebhookTrigger" +
                        "|Fail to parse the git web hook commit event|errMsg|${ignore.message}"
                )
                throw CustomException(
                    status = Response.Status.BAD_REQUEST,
                    message = "Fail to parse the git web hook commit event, errMsg: ${ignore.message}"
                )
            }
            ScmType.GITHUB -> {
                when (triggerBuildReq.eventType) {
                    GithubPushEvent.classType -> objectMapper.readValue<GithubPushEvent>(eventStr)
                    GithubPullRequestEvent.classType -> objectMapper.readValue<GithubPullRequestEvent>(eventStr)
                    else -> {
                        logger.info("Github event(${triggerBuildReq.eventType}) is ignored")
                        throw CustomException(
                            status = Response.Status.BAD_REQUEST,
                            message = "event not support"
                        )
                    }
                }
            }
            else -> throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "event not support"
            )
        }
    }

    private fun loadManualOpenApiAction(
        streamTriggerSetting: StreamTriggerSetting,
        userId: String,
        triggerBuildReq: TriggerBuildReq
    ): StreamOpenApiAction {
        val action = StreamOpenApiAction(
            actionFactory.loadManualAction(
                setting = streamTriggerSetting,
                event = StreamManualEvent(
                    userId = userId,
                    gitProjectId = GitCommonUtils.getGitProjectId(triggerBuildReq.projectId).toString(),
                    triggerBuildReq = triggerBuildReq
                )
            ),
            triggerBuildReq.checkPipelineTrigger
        )
        val request = action.buildRequestEvent("")?.copy(objectKind = StreamGitObjectKind.OBJECT_KIND_OPENAPI)
            ?: throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "event invalid"
            )
        val id = gitRequestEventDao.saveGitRequest(dslContext, request)
        action.data.context.requestEventId = id

        return action
    }
}
