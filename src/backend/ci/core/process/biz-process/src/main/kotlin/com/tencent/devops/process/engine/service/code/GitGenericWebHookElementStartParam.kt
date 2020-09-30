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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitGenericWebHookTriggerElement
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.ScmWebhookStartParams
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_COMMIT_TIME
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_EXCLUDE_PATHS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_EXCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_INCLUDE_USERS
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_RELATIVE_PATH
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_REVERSION
import com.tencent.devops.scm.pojo.BK_REPO_SVN_WEBHOOK_USERNAME

class GitGenericWebHookElementStartParam(
    private val projectId: String,
    private val repo: Repository,
    private val params: ScmWebhookMatcher.WebHookParams,
    private val matcher: ScmWebhookMatcher,
    private val matchResult: ScmWebhookMatcher.MatchResult
) : ScmWebhookStartParams<CodeGitGenericWebHookTriggerElement> {
    override fun getStartParams(element: CodeGitGenericWebHookTriggerElement): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        when (element.data.input.scmType) {
            ScmType.CODE_GIT.name, ScmType.CODE_TGIT.name ->
                gitStartParam(element = element, startParams = startParams)
            ScmType.CODE_GITLAB.name ->
                gitlabStartParam(element = element, startParams = startParams)
            ScmType.GITHUB.name ->
                githubStartParam(element = element, startParams = startParams)
            ScmType.CODE_SVN.name ->
                svnStartParam(element = element, startParams = startParams)
            else ->
                throw RuntimeException("Unknown scm type")
        }
        return startParams
    }

    private fun svnStartParam(
        element: CodeGitGenericWebHookTriggerElement,
        startParams: MutableMap<String, Any>
    ) {
        val svnMatcher = matcher as SvnWebHookMatcher
        val svnEvent = matcher.event
        startParams[BK_REPO_SVN_WEBHOOK_REVERSION] = svnMatcher.getRevision()
        startParams[BK_REPO_SVN_WEBHOOK_USERNAME] = svnMatcher.getUsername()
        startParams[BK_REPO_SVN_WEBHOOK_COMMIT_TIME] = svnEvent.commitTime ?: 0L
        with(element.data.input) {
            startParams[BK_REPO_SVN_WEBHOOK_RELATIVE_PATH] = relativePath ?: ""
            startParams[BK_REPO_SVN_WEBHOOK_EXCLUDE_PATHS] = excludePaths ?: ""
            startParams[BK_REPO_SVN_WEBHOOK_INCLUDE_USERS] = includeUsers?.joinToString(",") ?: ""
            startParams[BK_REPO_SVN_WEBHOOK_EXCLUDE_USERS] = excludeUsers?.joinToString(",") ?: ""
        }
    }

    private fun githubStartParam(
        element: CodeGitGenericWebHookTriggerElement,
        startParams: MutableMap<String, Any>
    ) {
        with(element.data.input) {
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
            startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = branchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = excludeBranchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = excludeUsers ?: ""
        }
        val githubWebhookStartParams = GithubWebHookStartParam(
            params = params,
            matcher = matcher as GithubWebHookMatcher
        )
        githubWebhookStartParams.getEventTypeStartParams(startParams)
    }

    private fun gitStartParam(
        element: CodeGitGenericWebHookTriggerElement,
        startParams: MutableMap<String, Any>
    ) {
        with(element.data.input) {
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = matcher.getRevision()
            startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = params.eventType ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = branchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS] = excludeBranchName ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS] = includePaths ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS] = excludePaths ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS] = excludeUsers?.joinToString(",") ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] =
                matchResult.extra[GitWebHookMatcher.MATCH_BRANCH] ?: ""
            startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH] = matchResult.extra[GitWebHookMatcher.MATCH_PATHS] ?: ""
        }
        val gitWebHookStartParam = GitWebHookStartParam(
            projectId = projectId,
            repo = repo,
            params = params,
            matcher = matcher as GitWebHookMatcher,
            matchResult = matchResult
        )
        gitWebHookStartParam.getEventTypeStartParams(startParams)
    }

    private fun gitlabStartParam(
        element: CodeGitGenericWebHookTriggerElement,
        startParams: MutableMap<String, Any>
    ) {
        with(element.data.input) {
            startParams[BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS] = branchName ?: ""
        }
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = matcher.getBranchName() ?: ""
    }
}