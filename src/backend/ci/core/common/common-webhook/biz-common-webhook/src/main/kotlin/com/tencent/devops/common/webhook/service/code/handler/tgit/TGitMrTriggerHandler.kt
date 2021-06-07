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

package com.tencent.devops.common.webhook.service.code.handler.tgit

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.constant.GIT_MR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.service.code.GitScmService
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.service.code.filter.PathPrefixFilter
import com.tencent.devops.common.webhook.service.code.filter.SkipCiFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.WebhookUtils.convert
import com.tencent.devops.common.webhook.util.WebhookUtils.getBranch
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.pojo.BK_REPO_GIT_MANUAL_UNLOCK
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class TGitMrTriggerHandler(
    private val gitScmService: GitScmService
) : GitHookTriggerHandler<GitMergeRequestEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitMrTriggerHandler::class.java)
    }

    override fun eventClass(): Class<GitMergeRequestEvent> {
        return GitMergeRequestEvent::class.java
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.MERGE_REQUEST
    }

    override fun getUrl(event: GitMergeRequestEvent): String {
        return event.object_attributes.target.http_url
    }

    override fun getUsername(event: GitMergeRequestEvent): String {
        return event.user.username
    }

    override fun getAction(event: GitMergeRequestEvent): String? {
        return event.object_attributes.action
    }

    override fun getRevision(event: GitMergeRequestEvent): String {
        return event.object_attributes.last_commit.id
    }

    override fun getRepoName(event: GitMergeRequestEvent): String {
        return GitUtils.getProjectName(event.object_attributes.target.ssh_url)
    }

    override fun getBranchName(event: GitMergeRequestEvent): String {
        return event.object_attributes.target_branch
    }

    override fun getMessage(event: GitMergeRequestEvent): String {
        return event.object_attributes.last_commit.message
    }

    override fun getHookSourceUrl(event: GitMergeRequestEvent): String {
        return event.object_attributes.source.http_url
    }

    override fun getHookTargetUrl(event: GitMergeRequestEvent): String {
        return event.object_attributes.target.http_url
    }

    override fun getMergeRequestId(event: GitMergeRequestEvent): Long {
        return event.object_attributes.id
    }

    override fun getEnv(event: GitMergeRequestEvent): Map<String, Any> {
        return mapOf(
            GIT_MR_NUMBER to event.object_attributes.iid,
            BK_REPO_GIT_MANUAL_UNLOCK to (event.manual_unlock ?: false)
        )
    }

    override fun preMatch(event: GitMergeRequestEvent): ScmWebhookMatcher.MatchResult {
        if (event.object_attributes.action == "close" ||
            (event.object_attributes.action == "update" &&
                event.object_attributes.extension_action != "push-update")
        ) {
            logger.info("Git web hook is ${event.object_attributes.action} merge request")
            return ScmWebhookMatcher.MatchResult(false)
        }
        return ScmWebhookMatcher.MatchResult(true)
    }

    override fun getEventFilters(
        event: GitMergeRequestEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val sourceBranchFilter = BranchFilter(
                pipelineId = pipelineId,
                triggerOnBranchName = getBranch(event.object_attributes.source_branch),
                includedBranches = convert(includeSourceBranchName),
                excludedBranches = convert(excludeSourceBranchName)
            )
            val skipCiFilter = SkipCiFilter(
                pipelineId = pipelineId,
                triggerOnMessage = event.object_attributes.last_commit.message
            )
            // 只有开启路径匹配时才查询mr change file list
            val mrChangeInfo = if (excludePaths.isNullOrBlank() && includePaths.isNullOrBlank()) {
                null
            } else {
                val mrId = if (repository is CodeGitlabRepository) {
                    event.object_attributes.iid
                } else {
                    event.object_attributes.id
                }
                gitScmService.getMergeRequestChangeInfo(projectId, mrId, repository)
            }
            val changeFiles = mrChangeInfo?.files?.map {
                if (it.deletedFile) {
                    it.oldPath
                } else {
                    it.newPath
                }
            } ?: emptyList()
            val pathFilter = PathPrefixFilter(
                pipelineId = pipelineId,
                triggerOnPath = changeFiles,
                includedPaths = convert(includePaths),
                excludedPaths = convert(excludePaths)
            )
            return listOf(sourceBranchFilter, skipCiFilter, pathFilter)
        }
    }
}
