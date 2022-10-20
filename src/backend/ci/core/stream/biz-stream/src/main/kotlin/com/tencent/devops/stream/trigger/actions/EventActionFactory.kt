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

package com.tencent.devops.stream.trigger.actions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.data.context.StreamTriggerContext
import com.tencent.devops.stream.trigger.actions.github.GithubPRActionGit
import com.tencent.devops.stream.trigger.actions.github.GithubPushActionGit
import com.tencent.devops.stream.trigger.actions.github.GithubTagPushActionGit
import com.tencent.devops.stream.trigger.actions.streamActions.StreamDeleteAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamManualAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamRepoTriggerAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamScheduleAction
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualEvent
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamScheduleEvent
import com.tencent.devops.stream.trigger.actions.tgit.TGitIssueActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitMrActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitNoteActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitPushActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitReviewActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitTagPushActionGit
import com.tencent.devops.stream.trigger.git.service.GithubApiService
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.service.DeleteEventService
import com.tencent.devops.stream.trigger.service.GitCheckService
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.trigger.service.StreamTriggerTokenService
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EventActionFactory @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val tGitApiService: TGitApiService,
    private val githubApiService: GithubApiService,
    private val streamEventService: StreamEventService,
    private val streamTimerService: StreamTimerService,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamDeleteEventService: DeleteEventService,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val basicSettingDao: StreamBasicSettingDao,
    private val pipelineDelete: PipelineDelete,
    private val gitCheckService: GitCheckService,
    private val mrConflictCheck: MergeConflictCheck,
    private val streamGitConfig: StreamGitConfig,
    private val streamTriggerTokenService: StreamTriggerTokenService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val redisOperation: RedisOperation,
    private val streamTriggerCache: StreamTriggerCache
) {

    companion object {
        private val logger = LoggerFactory.getLogger(EventActionFactory::class.java)
    }

    fun load(event: CodeWebhookEvent): BaseAction? {
        val action = loadEvent(event) ?: return null

        return action.init()
    }

    fun loadByData(
        eventStr: String,
        actionCommonData: EventCommonData,
        actionContext: StreamTriggerContext,
        actionSetting: StreamTriggerSetting?
    ): BaseAction? {
        val event = when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> {
                try {
                    objectMapper.readValue<GitEvent>(eventStr)
                } catch (ignore: Exception) {
                    logger.warn(
                        "EventActionFactory|loadByData" +
                            "|Fail to parse the git web hook commit event|errMsg|${ignore.message}"
                    )
                    return null
                }
            }
            ScmType.GITHUB -> {
                when (actionCommonData.eventType) {
                    GithubPushEvent.classType -> objectMapper.readValue<GithubPushEvent>(eventStr)
                    GithubPullRequestEvent.classType -> objectMapper.readValue<GithubPullRequestEvent>(eventStr)
                    else -> {
                        logger.info("Github event(${actionCommonData.eventType}) is ignored")
                        return null
                    }
                }
            }
            else -> TODO("对接其他Git平台时需要补充")
        }

        return loadByData(event, actionCommonData, actionContext, actionSetting)
    }

    fun loadByData(
        event: CodeWebhookEvent,
        actionCommonData: EventCommonData,
        actionContext: StreamTriggerContext,
        actionSetting: StreamTriggerSetting?
    ): BaseAction? {
        val action = loadEvent(event) ?: return null
        action.data.eventCommon = actionCommonData
        action.data.context = actionContext
        if (actionSetting != null) {
            action.data.setting = actionSetting
        }
        return if (actionContext.repoTrigger != null) {
            StreamRepoTriggerAction(
                baseAction = action,
                client = client,
                streamGitConfig = streamGitConfig,
                streamBasicSettingService = streamBasicSettingService,
                redisOperation = redisOperation,
                streamTriggerCache = streamTriggerCache
            )
        } else action
    }

    fun loadEvent(event: String, scmType: ScmType, objectKind: String): CodeWebhookEvent = when (scmType) {
        ScmType.CODE_GIT -> {
            objectMapper.readValue<GitEvent>(event)
        }
        ScmType.GITHUB -> {
            when (objectKind) {
                StreamObjectKind.PULL_REQUEST.value -> objectMapper.readValue<GithubPullRequestEvent>(event)
                StreamObjectKind.PUSH.value -> objectMapper.readValue<GithubPushEvent>(event)
                StreamObjectKind.TAG_PUSH.value -> objectMapper.readValue<GithubPushEvent>(event)
                else -> throw IllegalArgumentException("$objectKind in github load action not support yet")
            }
        }
        else -> TODO("对接其他Git平台时需要补充")
    }

    @Suppress("ComplexMethod")
    private fun loadEvent(event: CodeWebhookEvent): BaseAction? {
        // 先根据git事件分为得到初始化的git action
        val gitAction = when (event) {
            is GitPushEvent -> {
                val tGitPushAction = TGitPushActionGit(
                    dslContext = dslContext,
                    apiService = tGitApiService,
                    streamEventService = streamEventService,
                    streamTimerService = streamTimerService,
                    streamPipelineBranchService = streamPipelineBranchService,
                    streamDeleteEventService = streamDeleteEventService,
                    gitPipelineResourceDao = gitPipelineResourceDao,
                    pipelineDelete = pipelineDelete,
                    gitCheckService = gitCheckService
                )
                tGitPushAction
            }
            is GitMergeRequestEvent -> {
                val tGitMrAction = TGitMrActionGit(
                    dslContext = dslContext,
                    streamSettingDao = basicSettingDao,
                    apiService = tGitApiService,
                    mrConflictCheck = mrConflictCheck,
                    pipelineDelete = pipelineDelete,
                    gitCheckService = gitCheckService,
                    streamTriggerTokenService = streamTriggerTokenService
                )
                tGitMrAction
            }
            is GitTagPushEvent -> {
                val tGitTagPushAction = TGitTagPushActionGit(
                    apiService = tGitApiService,
                    gitCheckService = gitCheckService
                )
                tGitTagPushAction
            }
            is GitIssueEvent -> {
                val tGitIssueAction = TGitIssueActionGit(
                    dslContext = dslContext,
                    apiService = tGitApiService,
                    gitCheckService = gitCheckService,
                    basicSettingDao = basicSettingDao
                )
                tGitIssueAction
            }
            is GitReviewEvent -> {
                val tGitReviewAction = TGitReviewActionGit(
                    dslContext = dslContext,
                    apiService = tGitApiService,
                    gitCheckService = gitCheckService,
                    basicSettingDao = basicSettingDao
                )
                tGitReviewAction
            }
            is GitNoteEvent -> {
                val tGitNoteAction = TGitNoteActionGit(
                    dslContext = dslContext,
                    apiService = tGitApiService,
                    gitCheckService = gitCheckService,
                    basicSettingDao = basicSettingDao
                )
                tGitNoteAction
            }
            is GithubPushEvent -> {
                when {
                    event.ref.startsWith("refs/heads/") -> GithubPushActionGit(
                        dslContext = dslContext,
                        client = client,
                        apiService = githubApiService,
                        streamEventService = streamEventService,
                        streamTimerService = streamTimerService,
                        streamPipelineBranchService = streamPipelineBranchService,
                        streamDeleteEventService = streamDeleteEventService,
                        gitPipelineResourceDao = gitPipelineResourceDao,
                        pipelineDelete = pipelineDelete,
                        gitCheckService = gitCheckService
                    )
                    event.ref.startsWith("refs/tags/") -> GithubTagPushActionGit(
                        apiService = githubApiService,
                        gitCheckService = gitCheckService
                    )
                    else -> return null
                }
            }
            is GithubPullRequestEvent -> {
                GithubPRActionGit(
                    apiService = githubApiService,
                    mrConflictCheck = mrConflictCheck,
                    pipelineDelete = pipelineDelete,
                    gitCheckService = gitCheckService,
                    streamTriggerTokenService = streamTriggerTokenService,
                    basicSettingDao = basicSettingDao,
                    dslContext = dslContext
                )
            }
            else -> {
                return null
            }
        }
        gitAction.data = ActionData(event, StreamTriggerContext())

        // 再根据stream独有的git action抽象出来
        val action = when {
            gitAction.isStreamDeleteAction() -> {
                StreamDeleteAction(gitAction)
            }
            else -> gitAction
        }

        return action
    }

    fun loadManualAction(
        setting: StreamTriggerSetting,
        event: StreamManualEvent
    ): StreamManualAction {
        val streamManualAction = StreamManualAction(streamGitConfig)
        streamManualAction.data = ActionData(event, StreamTriggerContext())
        streamManualAction.api = when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> tGitApiService
            ScmType.GITHUB -> githubApiService
            else -> TODO("对接其他Git平台时需要补充")
        }
        streamManualAction.data.setting = setting
        streamManualAction.init()
        return streamManualAction
    }

    fun loadScheduleAction(
        setting: StreamTriggerSetting,
        event: StreamScheduleEvent
    ): StreamScheduleAction {
        val streamScheduleAction = StreamScheduleAction(streamGitConfig, gitCheckService)
        streamScheduleAction.data = ActionData(event, StreamTriggerContext())

        streamScheduleAction.api = when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> tGitApiService
            ScmType.GITHUB -> githubApiService
            else -> TODO("对接其他Git平台时需要补充")
        }
        streamScheduleAction.data.setting = setting
        streamScheduleAction.init()
        return streamScheduleAction
    }
}
