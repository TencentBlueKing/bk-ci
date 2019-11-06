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

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.scm.code.GitlabCommitEvent
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.Repository
import org.slf4j.LoggerFactory

class GitlabWebHookMatcher(private val event: GitlabCommitEvent) : ScmWebhookMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(GitlabWebHookMatcher::class.java)
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): ScmWebhookMatcher.MatchResult {
        with(webHookParams) {
            if (repository !is CodeGitlabRepository) {
                logger.warn("The repo($repository) is not code git repo for git web hook")
                return ScmWebhookMatcher.MatchResult(false)
            }
            if (repository.url != event.repository.git_http_url) {
                return ScmWebhookMatcher.MatchResult(false)
            }

            if (branchName.isNullOrEmpty()) {
                return ScmWebhookMatcher.MatchResult(true)
            }

            val match = isBranchMatch(branchName!!, event.ref)
            if (!match) {
                logger.info("The branch($branchName) is not match the git update one(${event.ref})")
            }
            return ScmWebhookMatcher.MatchResult(match)
        }
    }

    override fun getUsername() = event.user_name

    override fun getRevision() = event.checkout_sha

    override fun getRepoName() = event.project.path_with_namespace

    override fun getBranchName() = org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)

    override fun getEventType() = CodeEventType.PUSH

    override fun getCodeType() = CodeType.GITLAB

    override fun getMergeRequestId() = null
}