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

package com.tencent.devops.process.yaml.actions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteBranch
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.process.yaml.actions.data.ActionData
import com.tencent.devops.process.yaml.actions.data.EventCommonData
import com.tencent.devops.process.yaml.actions.data.PacRepoSetting
import com.tencent.devops.process.yaml.actions.data.context.PacTriggerContext
import com.tencent.devops.process.yaml.actions.internal.PipelineYamlDeleteAction
import com.tencent.devops.process.yaml.actions.internal.PipelineYamlManualAction
import com.tencent.devops.process.yaml.actions.internal.event.PipelineYamlManualEvent
import com.tencent.devops.process.yaml.actions.tgit.TGitIssueActionGit
import com.tencent.devops.process.yaml.actions.tgit.TGitMrActionGit
import com.tencent.devops.process.yaml.actions.tgit.TGitNoteActionGit
import com.tencent.devops.process.yaml.actions.tgit.TGitPushActionGit
import com.tencent.devops.process.yaml.actions.tgit.TGitReviewActionGit
import com.tencent.devops.process.yaml.actions.tgit.TGitTagPushActionGit
import com.tencent.devops.process.yaml.git.service.TGitApiService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EventActionFactory @Autowired constructor(
    private val tGitApiService: TGitApiService,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val pipelineYamlService: PipelineYamlService
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
        actionContext: PacTriggerContext,
        actionSetting: PacRepoSetting
    ): BaseAction? {
        return try {
            val action = loadEvent(event = objectMapper.readValue<GitEvent>(eventStr)) ?: return null
            action.data.eventCommon = actionCommonData
            action.data.context = actionContext
            action.data.setting = actionSetting
            action
        } catch (ignore: Exception) {
            logger.warn("EventActionFactory|loadByData|Fail to parse eventStr", ignore)
            null
        }
    }

    @Suppress("ComplexMethod")
    private fun loadEvent(event: CodeWebhookEvent): BaseAction? {
        // 先根据git事件分为得到初始化的git action
        val gitAction = when (event) {
            is GitPushEvent -> {
                val tGitPushAction = TGitPushActionGit(
                    apiService = tGitApiService
                )
                tGitPushAction
            }
            is GitMergeRequestEvent -> {
                val tGitMrAction = TGitMrActionGit(
                    apiService = tGitApiService,
                    pipelineYamlService = pipelineYamlService
                )
                tGitMrAction
            }
            is GitTagPushEvent -> {
                val tGitTagPushAction = TGitTagPushActionGit(
                    apiService = tGitApiService
                )
                tGitTagPushAction
            }
            is GitIssueEvent -> {
                val tGitIssueAction = TGitIssueActionGit(
                    apiService = tGitApiService,
                    client = client
                )
                tGitIssueAction
            }
            is GitReviewEvent -> {
                val tGitReviewAction = TGitReviewActionGit(
                    apiService = tGitApiService,
                    client = client
                )
                tGitReviewAction
            }
            is GitNoteEvent -> {
                val tGitNoteAction = TGitNoteActionGit(
                    apiService = tGitApiService,
                    client = client
                )
                tGitNoteAction
            }
            else -> {
                return null
            }
        }
        gitAction.data = ActionData(event, PacTriggerContext())

        return if (event is GitPushEvent && event.isDeleteBranch()) {
            PipelineYamlDeleteAction(gitAction = gitAction, pipelineYamlService = pipelineYamlService)
        } else {
            gitAction
        }
    }

    fun loadManualEvent(
        setting: PacRepoSetting,
        event: PipelineYamlManualEvent
    ): PipelineYamlManualAction {
        val pipelineYamlManualAction = PipelineYamlManualAction()
        pipelineYamlManualAction.data = ActionData(event, PacTriggerContext())
        pipelineYamlManualAction.api = when (event.scmType) {
            ScmType.CODE_GIT -> tGitApiService
            else -> TODO("对接其他代码库平台时需要补充")
        }
        pipelineYamlManualAction.data.setting = setting
        pipelineYamlManualAction.init()
        return pipelineYamlManualAction
    }

    fun loadManualEvent(
        eventStr: String,
        actionCommonData: EventCommonData,
        actionContext: PacTriggerContext,
        actionSetting: PacRepoSetting
    ): BaseAction? {
        val event = objectMapper.readValue<PipelineYamlManualEvent>(eventStr)
        val pipelineYamlManualAction = PipelineYamlManualAction()

        pipelineYamlManualAction.api = when (event.scmType) {
            ScmType.CODE_GIT -> tGitApiService
            else -> TODO("对接其他代码库平台时需要补充")
        }
        pipelineYamlManualAction.data = ActionData(event, actionContext)
        pipelineYamlManualAction.data.eventCommon = actionCommonData
        pipelineYamlManualAction.data.setting = actionSetting
        return pipelineYamlManualAction
    }
}
