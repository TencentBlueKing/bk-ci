/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
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
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import org.slf4j.LoggerFactory

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

    override fun getEventDesc(event: SvnCommitEvent): String {
        return I18Variable(
            code = WebhookI18nConstants.SVN_COMMIT_EVENT_DESC,
            params = listOf(
                getRevision(event),
                getUsername(event)
            )
        ).toJsonStr()
    }

    override fun getExternalId(event: SvnCommitEvent): String {
        return event.rep_name
    }

    override fun getCompatibilityRepoName(event: SvnCommitEvent): Set<String> {
        return event.repository?.svnHttpUrl?.let {
            val svnProjectName = SvnUtils.getSvnProjectName(it)
            if (svnProjectName != getRepoName(event)) {
                setOf(svnProjectName)
            } else {
                setOf()
            }
        } ?: setOf()
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
                triggerOnProjectNames = getCompatibilityRepoName(event).plus(getRepoName(event))
            )
            val userId = getUsername(event)
            val userFilter = UserFilter(
                pipelineId = pipelineId,
                triggerOnUser = userId,
                includedUsers = WebhookUtils.convert(includeUsers),
                excludedUsers = WebhookUtils.convert(excludeUsers),
                includedFailedReason = I18Variable(
                    code = WebhookI18nConstants.USER_NOT_MATCH,
                    params = listOf(userId)
                ).toJsonStr(),
                excludedFailedReason = I18Variable(
                    code = WebhookI18nConstants.USER_IGNORED,
                    params = listOf(userId)
                ).toJsonStr()
            )
            val projectRelativePath = WebhookUtils.getRelativePath(repository.url)
            val pathFilter = PathFilterFactory.newPathFilter(
                PathFilterConfig(
                    pathFilterType = pathFilterType,
                    pipelineId = pipelineId,
                    triggerOnPath = event.getMatchPaths(),
                    excludedPaths = WebhookUtils.convert(excludePaths).map { path ->
                        WebhookUtils.getFullPath(
                            projectRelativePath = projectRelativePath,
                            relativeSubPath = path
                        )
                    },
                    includedPaths = WebhookUtils.getSvnIncludePaths(webHookParams, projectRelativePath),
                    includedFailedReason = I18Variable(
                        code = WebhookI18nConstants.PATH_NOT_MATCH,
                        params = listOf()
                    ).toJsonStr(),
                    excludedFailedReason = I18Variable(
                        code = WebhookI18nConstants.PATH_IGNORED,
                        params = listOf()
                    ).toJsonStr()
                )
            )
            return listOf(projectNameFilter, userFilter, pathFilter)
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

    fun SvnCommitEvent.getMatchPaths() = if ((totalFilesCount ?: 0) < FILES_COUNT_MAX) {
        files.map { it.file }
    } else {
        // 超过上限则存在变更记录丢失, 用paths进行匹配
        logger.info("File change information exceeds the limit|$totalFilesCount")
        paths
    }

    companion object {
        // 文件变更列表上限
        const val FILES_COUNT_MAX = 999
        private val logger = LoggerFactory.getLogger(SvnCommitTriggerHandler::class.java)
    }
}
