/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
import com.tencent.devops.process.pojo.code.github.GithubCreateEvent
import com.tencent.devops.process.pojo.code.github.GithubEvent
import com.tencent.devops.process.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.process.pojo.code.github.GithubPushEvent
import com.tencent.devops.process.utils.GITHUB_PR_NUMBER
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.github.GithubRepository
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class GithubWebHookMatcher(val event: GithubEvent) : ScmWebhookMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubWebHookMatcher::class.java)
        private val regex = Pattern.compile("[,;]")
    }

    override fun isMatch(
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): ScmWebhookMatcher.MatchResult {
        with(webHookParams) {
            if (repository !is GithubRepository) {
                logger.warn("The repo($repository) is not code git repo for github web hook")
                return ScmWebhookMatcher.MatchResult(false)
            }
            if (!matchUrl(repository.url)) {
                logger.info("The repo($repository) is not match event($event)")
                return ScmWebhookMatcher.MatchResult(false)
            }

            val eventUsername = getUsername()
            val eventBranch = getBranch()

            // 检测事件类型是否符合
            if (eventType != null && eventType != getEventType()) {
                logger.info("Git web hook event($event) (${getEventType()}) not match $eventType")
                return ScmWebhookMatcher.MatchResult(false)
            }

            if (excludeUsers != null) {
                val excludeUserSet = regex.split(excludeUsers)
                excludeUserSet.forEach {
                    if (it == eventUsername) {
                        logger.info("The exclude user($excludeUsers) exclude the git update one($eventBranch)")
                        return ScmWebhookMatcher.MatchResult(false)
                    }
                }
            }

            if (eventType == CodeEventType.CREATE) {
                return ScmWebhookMatcher.MatchResult(true)
            }

            if (excludeBranchName != null) {
                val excludeBranchNameSet = regex.split(excludeBranchName).toSet()
                excludeBranchNameSet.forEach {
                    if (isBranchMatch(it, eventBranch)) {
                        logger.info("The exclude branch($excludeBranchName) exclude the git update one($eventBranch)")
                        return ScmWebhookMatcher.MatchResult(false)
                    }
                }
            }

            if (branchName.isNullOrBlank()) {
                logger.info("Git trigger ignore branch name")
                return ScmWebhookMatcher.MatchResult(true)
            } else {
                val includeBranchNameSet = regex.split(branchName)
                includeBranchNameSet.forEach {
                    if (isBranchMatch(it, eventBranch)) {
                        logger.info("The include branch($branchName) include the git update one($eventBranch)")
                        return ScmWebhookMatcher.MatchResult(true)
                    }
                }
            }

            logger.info("The include branch($branchName) doesn't include the git update one($eventBranch)")
            return ScmWebhookMatcher.MatchResult(false)
        }
    }

    private fun matchUrl(url: String): Boolean {
        return when (event) {
            is GithubPushEvent -> {
                url == event.repository.ssh_url || url == event.repository.clone_url
            }
            is GithubCreateEvent -> {
                url == event.repository.ssh_url || url == event.repository.clone_url
            }
            is GithubPullRequestEvent -> {
                url == event.repository.ssh_url || url == event.repository.clone_url
            }
            else -> {
                false
            }
        }
    }

    private fun getBranch(): String {
        return when (event) {
            is GithubPushEvent -> event.ref
            is GithubCreateEvent -> event.ref
            is GithubPullRequestEvent -> event.pull_request.base.ref
            else -> ""
        }
    }

    override fun getUsername(): String {
        return when (event) {
            is GithubPushEvent -> event.sender.login
            is GithubCreateEvent -> event.sender.login
            is GithubPullRequestEvent -> event.sender.login
            else -> ""
        }
    }

    override fun getRevision(): String {
        return when (event) {
            is GithubPushEvent -> event.head_commit.id
            is GithubPullRequestEvent -> event.pull_request.head.sha
            else -> ""
        }
    }

    override fun getEventType(): CodeEventType {
        return when (event) {
            is GithubPushEvent -> CodeEventType.PUSH
            is GithubCreateEvent -> CodeEventType.CREATE
            is GithubPullRequestEvent -> CodeEventType.PULL_REQUEST
            else -> CodeEventType.PUSH
        }
    }

    override fun getHookSourceUrl(): String? {
        return if (event is GithubPullRequestEvent) event.pull_request.head.repo.clone_url else null
    }

    override fun getHookTargetUrl(): String? {
        return if (event is GithubPullRequestEvent) event.pull_request.base.repo.clone_url else null
    }

    override fun getCodeType() = CodeType.GITHUB

    override fun getRepoName(): String {
        val sshUrl = when (event) {
            is GithubPushEvent -> event.repository.ssh_url
            is GithubCreateEvent -> event.repository.ssh_url
            is GithubPullRequestEvent -> event.repository.ssh_url
            else -> ""
        }
        return sshUrl.removePrefix("git@github.com:").removeSuffix(".git")
    }

    override fun getBranchName(): String {
        return when (event) {
            is GithubPushEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GithubCreateEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
            is GithubPullRequestEvent -> org.eclipse.jgit.lib.Repository.shortenRefName(event.pull_request.head.ref)
            else -> ""
        }
    }

    override fun getEnv(): Map<String, Any> {
        if (event is GithubPullRequestEvent) {
            return mapOf(GITHUB_PR_NUMBER to event.number)
        }
        return super.getEnv()
    }

    override fun getMergeRequestId() = null
}