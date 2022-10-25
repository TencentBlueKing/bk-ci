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

package com.tencent.devops.common.webhook.service.code.handler.svn

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_SVN_WEBHOOK_COMMIT_TIME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_SVN_WEBHOOK_REVERSION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_SVN_WEBHOOK_USERNAME
import com.tencent.devops.common.webhook.pojo.code.PathFilterConfig
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.common.webhook.service.code.filter.PathFilterFactory
import com.tencent.devops.common.webhook.service.code.filter.ProjectNameFilter
import com.tencent.devops.common.webhook.service.code.filter.UserFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.repository.pojo.Repository

@CodeWebhookHandler
@SuppressWarnings("TooManyFunctions")
class SvnCommitTriggerHandler : CodeWebhookTriggerHandler<SvnCommitEvent> {
    override fun eventClass(): Class<SvnCommitEvent> {
        return SvnCommitEvent::class.java
    }

    override fun getUrl(event: SvnCommitEvent): String {
        return event.rep_name
    }

    override fun getUsername(event: SvnCommitEvent): String {
        return event.userName
    }

    override fun getRevision(event: SvnCommitEvent): String {
        return event.revision.toString()
    }

    override fun getRepoName(event: SvnCommitEvent): String {
        return event.rep_name
    }

    override fun getBranchName(event: SvnCommitEvent): String {
        return ""
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.POST_COMMIT
    }

    override fun getMessage(event: SvnCommitEvent): String? {
        return event.log
    }

    override fun getWebhookFilters(
        event: SvnCommitEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val projectNameFilter = ProjectNameFilter(
                pipelineId = pipelineId,
                projectName = repository.projectName,
                triggerOnProjectName = event.rep_name
            )
            val userFilter = UserFilter(
                pipelineId = pipelineId,
                triggerOnUser = getUsername(event),
                includedUsers = WebhookUtils.convert(includeUsers),
                excludedUsers = WebhookUtils.convert(excludeUsers)
            )
            val projectRelativePath = WebhookUtils.getRelativePath(repository.url)
            val pathFilter = PathFilterFactory.newPathFilter(
                PathFilterConfig(
                    pathFilterType = pathFilterType,
                    pipelineId = pipelineId,
                    triggerOnPath = event.files.map { it.file },
                    excludedPaths = WebhookUtils.convert(excludePaths).map { path ->
                        WebhookUtils.getFullPath(
                            projectRelativePath = projectRelativePath,
                            relativeSubPath = path
                        )
                    },
                    includedPaths = getIncludePaths(projectRelativePath)
                )
            )
            return listOf(projectNameFilter, userFilter, pathFilter)
        }
    }

    private fun WebHookParams.getIncludePaths(projectRelativePath: String) =
        // 如果没有配置包含路径，则需要跟代码库url做对比
        if (relativePath.isNullOrBlank()) {
            // 模糊匹配需要包含整个路径
            if (pathFilterType == PathFilterType.NamePrefixFilter) {
                listOf(
                    WebhookUtils.getFullPath(
                        projectRelativePath = projectRelativePath,
                        relativeSubPath = ""
                    )
                )
            } else {
                listOf(
                    WebhookUtils.getFullPath(
                        projectRelativePath = projectRelativePath,
                        relativeSubPath = "**"
                    )
                )
            }
        } else {
            WebhookUtils.convert(relativePath).map { path ->
                WebhookUtils.getFullPath(
                    projectRelativePath = projectRelativePath,
                    relativeSubPath = path
                )
            }
        }

    override fun retrieveParams(
        event: SvnCommitEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_SVN_WEBHOOK_REVERSION] = event.revision.toString()
        startParams[BK_REPO_SVN_WEBHOOK_USERNAME] = event.userName
        startParams[BK_REPO_SVN_WEBHOOK_COMMIT_TIME] = event.commitTime ?: 0L
        return startParams
    }
}
