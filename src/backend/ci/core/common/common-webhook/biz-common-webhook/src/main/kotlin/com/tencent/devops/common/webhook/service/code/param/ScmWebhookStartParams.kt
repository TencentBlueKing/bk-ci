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

package com.tencent.devops.common.webhook.service.code.param

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_CODE_BASE_TRIGGERING
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_ALIAS_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BLOCK
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_QUEUE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils

@SuppressWarnings("LongParameterList")
interface ScmWebhookStartParams<T : WebHookTriggerElement> {

    fun elementClass(): Class<T>

    fun getStartParams(
        projectId: String,
        element: T,
        repo: Repository,
        matcher: ScmWebhookMatcher,
        variables: Map<String, String>,
        params: WebHookParams,
        matchResult: ScmWebhookMatcher.MatchResult
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams.putAll(
            getCommonStartParams(
                element = element,
                repo = repo,
                matcher = matcher,
                variables = variables,
                params = params
            )
        )
        startParams.putAll(
            getElementStartParams(
                projectId = projectId,
                element = element,
                repo = repo,
                matcher = matcher,
                variables = variables,
                params = params,
                matchResult = matchResult
            )
        )
        return startParams
    }

    private fun getCommonStartParams(
        element: T,
        repo: Repository,
        matcher: ScmWebhookMatcher,
        variables: Map<String, String>,
        params: WebHookParams
    ): Map<String, Any> {
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
        startParams[PIPELINE_BUILD_MSG] = startParams[PIPELINE_WEBHOOK_COMMIT_MESSAGE] as String?
            ?: I18nUtil.getCodeLanMessage(BK_CODE_BASE_TRIGGERING)
        startParams[PIPELINE_WEBHOOK_QUEUE] = params.webhookQueue

        val gitProjectName = matcher.getRepoName()
        startParams[PIPELINE_GIT_REPO] = gitProjectName
        val (group, name) = GitUtils.getRepoGroupAndName(gitProjectName)
        startParams[PIPELINE_GIT_REPO_NAME] = name
        startParams[PIPELINE_GIT_REPO_GROUP] = group
        return startParams
    }

    fun getElementStartParams(
        projectId: String,
        element: T,
        repo: Repository,
        matcher: ScmWebhookMatcher,
        variables: Map<String, String>,
        params: WebHookParams,
        matchResult: ScmWebhookMatcher.MatchResult
    ): Map<String, Any>
}
