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

package com.tencent.devops.process.trigger.actions

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.process.trigger.actions.data.ActionData
import com.tencent.devops.process.trigger.actions.data.PacRepoSetting
import com.tencent.devops.process.trigger.actions.data.context.PacTriggerContext
import com.tencent.devops.process.trigger.actions.pacActions.PacEnableAction
import com.tencent.devops.process.trigger.actions.pacActions.data.PacEnableEvent
import com.tencent.devops.process.trigger.actions.tgit.TGitIssueActionGit
import com.tencent.devops.process.trigger.actions.tgit.TGitNoteActionGit
import com.tencent.devops.process.trigger.actions.tgit.TGitPushActionGit
import com.tencent.devops.process.trigger.actions.tgit.TGitReviewActionGit
import com.tencent.devops.process.trigger.actions.tgit.TGitTagPushActionGit
import com.tencent.devops.process.trigger.git.service.TGitApiService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EventActionFactory @Autowired constructor(
    private val tGitApiService: TGitApiService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(EventActionFactory::class.java)
    }

    fun load(event: CodeWebhookEvent): BaseAction? {
        val action = loadEvent(event) ?: return null

        return action.init()
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
            is GitTagPushEvent -> {
                val tGitTagPushAction = TGitTagPushActionGit(
                    apiService = tGitApiService,
                )
                tGitTagPushAction
            }
            is GitIssueEvent -> {
                val tGitIssueAction = TGitIssueActionGit(
                    apiService = tGitApiService
                )
                tGitIssueAction
            }
            is GitReviewEvent -> {
                val tGitReviewAction = TGitReviewActionGit(
                    apiService = tGitApiService
                )
                tGitReviewAction
            }
            is GitNoteEvent -> {
                val tGitNoteAction = TGitNoteActionGit(
                    apiService = tGitApiService
                )
                tGitNoteAction
            }
            else -> {
                return null
            }
        }
        gitAction.data = ActionData(event, PacTriggerContext())

        return gitAction
    }

    fun loadEnableEvent(
        setting: PacRepoSetting,
        event: PacEnableEvent
    ): PacEnableAction {
        val pacEnableAction = PacEnableAction()
        pacEnableAction.data = ActionData(event, PacTriggerContext())
        pacEnableAction.api = when (event.scmType) {
            ScmType.CODE_GIT -> tGitApiService
            else -> TODO("对接其他代码库平台时需要补充")
        }
        pacEnableAction.data.setting = setting
        pacEnableAction.init()
        return pacEnableAction
    }
}
