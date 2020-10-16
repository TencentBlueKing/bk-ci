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

package com.tencent.devops.process.engine.service.code

import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.ScmWebhookStartParams
import com.tencent.devops.process.pojo.code.github.GithubCreateEvent
import com.tencent.devops.process.pojo.code.github.GithubPullRequest
import com.tencent.devops.process.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.scm.pojo.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME
import com.tencent.devops.scm.pojo.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_AUTHOR
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_LABELS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TITLE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_PUSH_USERNAME
import org.slf4j.LoggerFactory

class GithubWebHookStartParam(
    private val params: ScmWebhookMatcher.WebHookParams,
    private val matcher: GithubWebHookMatcher
) : ScmWebhookStartParams<CodeGithubWebHookTriggerElement> {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubWebHookStartParam::class.java)
    }

    override fun getStartParams(element: CodeGithubWebHookTriggerElement): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
        startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = element.branchName ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = element.excludeBranchName ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = element.excludeUsers ?: ""
        getEventTypeStartParams(startParams)
        return startParams
    }

    fun getEventTypeStartParams(startParams: MutableMap<String, Any>) {
        when (params.eventType) {
            CodeEventType.PULL_REQUEST ->
                pullRequestEventStartParam(startParams)
            CodeEventType.CREATE ->
                createEventStartParam(startParams)
            CodeEventType.PUSH ->
                pushEventStartParam(startParams)
            else ->
                logger.info("github webhook startparam eventType error, eventType:${params.eventType}, ignore")
        }
    }

    private fun pullRequestEventStartParam(startParams: MutableMap<String, Any>) {
        val githubEvent = matcher.event as GithubPullRequestEvent
        startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = githubEvent.sender.login
        startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = githubEvent.number
        pullRequestStartParam(pullRequest = githubEvent.pull_request, startParams = startParams)
    }

    private fun pullRequestStartParam(pullRequest: GithubPullRequest, startParams: MutableMap<String, Any>) {
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = pullRequest.base.repo.clone_url
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = pullRequest.head.repo.clone_url
        startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = pullRequest.base.ref
        startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = pullRequest.head.ref
        startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = pullRequest.created_at ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = pullRequest.update_at ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = pullRequest.id
        startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = pullRequest.comments_url ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = pullRequest.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] =
            pullRequest.assignees.joinToString(",") { it.login ?: "" }
        startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = pullRequest.url
        startParams[BK_REPO_GIT_WEBHOOK_MR_REVIEWERS] =
            pullRequest.requested_reviewers.joinToString(",") { it.login ?: "" }
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE] = pullRequest.milestone?.title ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE] =
            pullRequest.milestone?.due_on ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_MR_LABELS] =
            pullRequest.labels.joinToString(",") { it.name }
    }

    private fun createEventStartParam(startParams: MutableMap<String, Any>) {
        val githubEvent = matcher.event as GithubCreateEvent
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME] = githubEvent.ref
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE] = githubEvent.ref_type
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME] = githubEvent.sender.login
    }

    private fun pushEventStartParam(startParams: MutableMap<String, Any>) {
        startParams[BK_REPO_GIT_WEBHOOK_PUSH_USERNAME] = matcher.getUsername()
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = matcher.getBranchName()
    }
}