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

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.utils.PIPELINE_REPO_NAME
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_BLOCK
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_REPO
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_REPO_TYPE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_URL
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_URL
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_ALIAS_NAME
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_URL

object ScmWebhookParamsFactory {

    fun getWebhookElementParams(
        element: Element,
        variables: Map<String, String>
    ): ScmWebhookMatcher.WebHookParams? {
        return when (element) {
            is CodeSVNWebHookTriggerElement ->
                SvnWebhookElementParams().getWebhookElementParams(
                    element = element,
                    variables = variables
                )
            is CodeGithubWebHookTriggerElement ->
                GithubWebhookElementParams().getWebhookElementParams(
                    element = element,
                    variables = variables
                )
            is CodeGitWebHookTriggerElement ->
                GitWebhookElementParams().getWebhookElementParams(
                    element = element,
                    variables = variables
                )
            is CodeGitlabWebHookTriggerElement ->
                GitlabWebhookElementParams().getWebhookElementParams(
                    element = element,
                    variables = variables
                )
            is CodeTGitWebHookTriggerElement ->
                TGitWebhookElementParams().getWebhookElementParams(
                    element = element,
                    variables = variables
                )
            else -> {
                null
            }
        }
    }

    fun getStartParams(
        projectId: String,
        element: Element,
        repo: Repository,
        matcher: ScmWebhookMatcher,
        variables: Map<String, String>,
        params: ScmWebhookMatcher.WebHookParams,
        matchResult: ScmWebhookMatcher.MatchResult
    ): Map<String, Any> {
        val startParams = getCommonStartParams(matcher, element, params, variables, repo)
        startParams.putAll(getElementStartParams(element, matcher, params, projectId, repo, matchResult))
        return startParams
    }

    private fun getElementStartParams(
        element: Element,
        matcher: ScmWebhookMatcher,
        params: ScmWebhookMatcher.WebHookParams,
        projectId: String,
        repo: Repository,
        matchResult: ScmWebhookMatcher.MatchResult
    ): Map<String, Any> {
        return when (element) {
            is CodeSVNWebHookTriggerElement ->
                SvnWebHookStartParam(
                    matcher = matcher as SvnWebHookMatcher
                ).getStartParams(element)
            is CodeGithubWebHookTriggerElement ->
                GithubWebHookStartParam(
                    params = params,
                    matcher = matcher as GithubWebHookMatcher
                ).getStartParams(element)
            is CodeGitWebHookTriggerElement ->
                GitWebHookStartParam(
                    projectId = projectId,
                    repo = repo,
                    params = params,
                    matcher = matcher as GitWebHookMatcher,
                    matchResult = matchResult
                ).getStartParams(element)
            is CodeGitlabWebHookTriggerElement ->
                GitlabWebHookStartParam(
                    matcher = matcher
                ).getStartParams(element)
            is CodeTGitWebHookTriggerElement ->
                TGitWebHookStartParam(
                    projectId = projectId,
                    repo = repo,
                    params = params,
                    matcher = matcher as GitWebHookMatcher,
                    matchResult = matchResult
                ).getStartParams(element)
            else -> {
                emptyMap()
            }
        }
    }

    private fun getCommonStartParams(
        matcher: ScmWebhookMatcher,
        element: Element,
        params: ScmWebhookMatcher.WebHookParams,
        variables: Map<String, String>,
        repo: Repository
    ): MutableMap<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[PIPELINE_WEBHOOK_REVISION] = matcher.getRevision()
        startParams[PIPELINE_REPO_NAME] = matcher.getRepoName()
        startParams[PIPELINE_START_WEBHOOK_USER_ID] = matcher.getUsername()
        startParams[PIPELINE_START_TASK_ID] = element.id!! // 当前触发节点为启动节点
        startParams[PIPELINE_WEBHOOK_TYPE] = matcher.getCodeType().name
        startParams[PIPELINE_WEBHOOK_EVENT_TYPE] = matcher.getEventType().name

        startParams[PIPELINE_WEBHOOK_REPO] = params.repositoryConfig.getRepositoryId()
        startParams[PIPELINE_WEBHOOK_REPO_TYPE] = params.repositoryConfig.repositoryType.name
        startParams[PIPELINE_WEBHOOK_BLOCK] = params.block
        startParams.putAll(matcher.getEnv())
        startParams.putAll(variables)

        if (!matcher.getBranchName().isNullOrBlank()) {
            startParams[PIPELINE_WEBHOOK_BRANCH] = matcher.getBranchName()!!
        }
        if (!matcher.getHookSourceUrl().isNullOrBlank()) {
            startParams[PIPELINE_WEBHOOK_SOURCE_URL] = matcher.getHookSourceUrl()!!
        }
        if (!matcher.getHookTargetUrl().isNullOrBlank()) {
            startParams[PIPELINE_WEBHOOK_TARGET_URL] = matcher.getHookTargetUrl()!!
        }
        if (!matcher.getMessage().isNullOrBlank()) {
            val message = matcher.getMessage()!!
            startParams[PIPELINE_WEBHOOK_COMMIT_MESSAGE] = if (message.length >= 128) {
                message.substring(0, 128)
            } else {
                message
            }
        }

        // set new params
        startParams[BK_REPO_WEBHOOK_REPO_TYPE] = params.codeType.name
        startParams[BK_REPO_WEBHOOK_REPO_URL] = repo.url
        startParams[BK_REPO_WEBHOOK_REPO_NAME] = repo.projectName
        startParams[BK_REPO_WEBHOOK_REPO_ALIAS_NAME] = repo.aliasName
        startParams[BK_REPO_WEBHOOK_HASH_ID] = repo.repoHashId ?: ""
        return startParams
    }
}