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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.StreamDeleteAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamManualAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamScheduleAction
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualActionData
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualEvent
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamScheduleActionData
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamScheduleEvent
import com.tencent.devops.stream.trigger.actions.tgit.TGitIssueActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitMrActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitNoteActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitPushActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitReviewActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitTagPushActionGit
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitIssueActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitMrActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitNoteActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitPushActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitReviewActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitTagPushActionData
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.service.DeleteEventService
import com.tencent.devops.stream.trigger.service.GitCheckService
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EventActionFactory @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val tGitApiService: TGitApiService,
    private val streamEventService: StreamEventService,
    private val streamTimerService: StreamTimerService,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamDeleteEventService: DeleteEventService,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val pipelineDelete: PipelineDelete,
    private val gitCheckService: GitCheckService,
    private val mrConflictCheck: MergeConflictCheck,
    private val streamTriggerCache: StreamTriggerCache
) {

    fun load(event: CodeWebhookEvent): BaseAction? {
        // 先根据git事件分为得到初始化的git action
        val gitAction = when (event) {
            is GitPushEvent -> {
                val tGitPushAction = TGitPushActionGit(
                    dslContext = dslContext,
                    client = client,
                    apiService = tGitApiService,
                    streamEventService = streamEventService,
                    streamTimerService = streamTimerService,
                    streamPipelineBranchService = streamPipelineBranchService,
                    streamDeleteEventService = streamDeleteEventService,
                    gitPipelineResourceDao = gitPipelineResourceDao,
                    pipelineDelete = pipelineDelete,
                    gitCheckService = gitCheckService
                )
                tGitPushAction.data = TGitPushActionData(event)
                tGitPushAction.init()
            }
            is GitMergeRequestEvent -> {
                val tGitMrAction = TGitMrActionGit(
                    apiService = tGitApiService,
                    mrConflictCheck = mrConflictCheck,
                    pipelineDelete = pipelineDelete,
                    gitCheckService = gitCheckService
                )
                tGitMrAction.data = TGitMrActionData(event)
                tGitMrAction.init()
            }
            is GitTagPushEvent -> {
                val tGitTagPushAction = TGitTagPushActionGit(
                    apiService = tGitApiService,
                    gitCheckService = gitCheckService
                )
                tGitTagPushAction.data = TGitTagPushActionData(event)
                tGitTagPushAction.init()
            }
            is GitIssueEvent -> {
                val tGitIssueAction = TGitIssueActionGit(
                    apiService = tGitApiService,
                    streamTriggerCache = streamTriggerCache,
                    gitCheckService = gitCheckService,
                )
                tGitIssueAction.data = TGitIssueActionData(event)
                tGitIssueAction.init()
            }
            is GitReviewEvent -> {
                val tGitReviewAction = TGitReviewActionGit(
                    apiService = tGitApiService,
                    streamTriggerCache = streamTriggerCache,
                    gitCheckService = gitCheckService
                )
                tGitReviewAction.data = TGitReviewActionData(event)
                tGitReviewAction.init()
            }
            is GitNoteEvent -> {
                val tGitNoteAction = TGitNoteActionGit(
                    apiService = tGitApiService,
                    streamTriggerCache = streamTriggerCache,
                    gitCheckService = gitCheckService
                )
                tGitNoteAction.data = TGitNoteActionData(event)
                tGitNoteAction.init()
            }
            else -> {
                return null
            }
        }
        // 再根据stream独有的git action抽象出来
        return when {
            gitAction.isStreamDeleteAction() -> {
                StreamDeleteAction(gitAction)
            }
            else -> gitAction
        }
    }

    private fun GitBaseAction.init(): GitBaseAction {
        return this.initCommonData()
    }

    fun loadManualAction(
        setting: StreamTriggerSetting,
        event: StreamManualEvent
    ): StreamManualAction {
        val streamManualAction = StreamManualAction(streamTriggerCache)
        streamManualAction.data = StreamManualActionData(event)
        streamManualAction.api = when (setting.scmType) {
            ScmType.CODE_GIT -> tGitApiService
            else -> TODO()
        }
        streamManualAction.data.setting = setting

        // init common 需要api和setting
        return streamManualAction.initCommonData()
    }

    fun loadScheduleAction(
        setting: StreamTriggerSetting,
        event: StreamScheduleEvent
    ): StreamScheduleAction {
        val streamScheduleAction = StreamScheduleAction(gitCheckService)
        streamScheduleAction.data = StreamScheduleActionData(event)

        streamScheduleAction.api = when (setting.scmType) {
            ScmType.CODE_GIT -> tGitApiService
            else -> TODO()
        }
        streamScheduleAction.data.setting = setting

        return streamScheduleAction.initCommonData()
    }
}
