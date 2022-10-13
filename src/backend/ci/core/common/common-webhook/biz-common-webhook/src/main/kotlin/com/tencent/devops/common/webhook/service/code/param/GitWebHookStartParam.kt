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

import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_MANUAL_UNLOCK
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ENABLE_CHECK
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_AUTH_USER
import com.tencent.devops.common.webhook.pojo.code.MATCH_BRANCH
import com.tencent.devops.common.webhook.pojo.code.MATCH_PATHS
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitWebHookStartParam @Autowired constructor(
    private val eventCacheService: EventCacheService
) : ScmWebhookStartParams<CodeGitWebHookTriggerElement> {

    override fun elementClass(): Class<CodeGitWebHookTriggerElement> {
        return CodeGitWebHookTriggerElement::class.java
    }

    override fun getElementStartParams(
        projectId: String,
        element: CodeGitWebHookTriggerElement,
        repo: Repository,
        matcher: ScmWebhookMatcher,
        variables: Map<String, String>,
        params: WebHookParams,
        matchResult: ScmWebhookMatcher.MatchResult
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
        startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = element.branchName ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = element.excludeBranchName ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS] = element.includePaths ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS] = element.excludePaths ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = element.excludeUsers?.joinToString(",") ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] =
            matchResult.extra[MATCH_BRANCH] ?: ""
        startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH] = matchResult.extra[MATCH_PATHS] ?: ""
        startParams[BK_REPO_GIT_MANUAL_UNLOCK] = matcher.getEnv()[BK_REPO_GIT_MANUAL_UNLOCK] ?: false
        startParams[BK_REPO_GIT_WEBHOOK_ENABLE_CHECK] = element.enableCheck ?: true
        startParams[PIPELINE_GIT_REPO_ID] = element.repositoryName ?: ""
        startParams[BK_REPO_WEBHOOK_REPO_AUTH_USER] =
            if (repo is CodeGitRepository && repo.authType == RepoAuthType.OAUTH) {
                repo.userName
            } else {
                eventCacheService.getRepoAuthUser(projectId = projectId, repo = repo)
            }
        startParams.putAll(
            matcher.retrieveParams(
                projectId = projectId,
                repository = repo
            )
        )

        return startParams
    }
}
