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

package com.tencent.devops.process.engine.service.code.handler.tgit

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.process.engine.service.code.filter.BranchFilter
import com.tencent.devops.process.engine.service.code.filter.PathPrefixFilter
import com.tencent.devops.process.engine.service.code.filter.SkipCiFilter
import com.tencent.devops.process.engine.service.code.filter.WebhookFilter
import com.tencent.devops.process.engine.service.code.handler.WebhookUtils.convert
import com.tencent.devops.process.engine.service.code.handler.WebhookUtils.getBranch
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.WebHookEvent
import com.tencent.devops.process.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.service.scm.GitScmService
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.Repository
import org.springframework.stereotype.Service

@Service
class TGitMrTriggerHandler(
    private val gitScmService: GitScmService
) : TGitHookTriggerHandler {
    override fun canHandler(event: WebHookEvent): Boolean {
        return event is GitMergeRequestEvent
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.MERGE_REQUEST
    }

    override fun getUrl(event: WebHookEvent): String {
        return (event as GitMergeRequestEvent).object_attributes.target.http_url
    }

    override fun getUser(event: WebHookEvent): String {
        return (event as GitMergeRequestEvent).user.username
    }

    override fun getAction(event: WebHookEvent): String? {
        return (event as GitMergeRequestEvent).object_attributes.action
    }

    override fun getEventFilters(
        event: WebHookEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): List<WebhookFilter> {
        event as GitMergeRequestEvent
        with(webHookParams) {
            val targetBranchFilter = BranchFilter(
                pipelineId = pipelineId,
                triggerOnBranchName = getBranch(event.object_attributes.target_branch),
                includedBranches = convert(branchName),
                excludedBranches = convert(excludeBranchName)
            )
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
            return listOf(targetBranchFilter, sourceBranchFilter, skipCiFilter, pathFilter)
        }
    }
}
