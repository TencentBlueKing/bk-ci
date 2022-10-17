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

import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_COMMIT_ID
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
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TGitWebHookStartParam @Autowired constructor(
    private val gitScmService: GitScmService
) : ScmWebhookStartParams<CodeTGitWebHookTriggerElement> {

    override fun elementClass(): Class<CodeTGitWebHookTriggerElement> {
        return CodeTGitWebHookTriggerElement::class.java
    }

    override fun getElementStartParams(
        projectId: String,
        element: CodeTGitWebHookTriggerElement,
        repo: Repository,
        matcher: ScmWebhookMatcher,
        variables: Map<String, String>,
        params: WebHookParams,
        matchResult: ScmWebhookMatcher.MatchResult
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        with(element.data.input) {
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
            startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = branchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = excludeBranchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS] = includePaths ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS] = excludePaths ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = excludeUsers?.joinToString(",") ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] =
                matchResult.extra[MATCH_BRANCH] ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH] = matchResult.extra[MATCH_PATHS] ?: ""
            startParams[BK_REPO_WEBHOOK_REPO_AUTH_USER] =
                if (repo is CodeTGitRepository && repo.authType == RepoAuthType.OAUTH) {
                    repo.userName
                } else {
                    gitScmService.getRepoAuthUser(projectId = projectId, repo = repo)
                }
            startParams.putAll(
                matcher.retrieveParams(
                    projectId = projectId,
                    repository = repo
                )
            )
        }
        return startParams
    }
}
