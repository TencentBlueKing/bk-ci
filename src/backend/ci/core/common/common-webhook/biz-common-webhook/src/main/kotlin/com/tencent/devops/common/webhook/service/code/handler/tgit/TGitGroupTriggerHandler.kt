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

package com.tencent.devops.common.webhook.service.code.handler.tgit

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.enums.code.tgit.TGitProjectOperation
import com.tencent.devops.common.webhook.pojo.code.PathFilterConfig
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitGroupEvent
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.PathFilterFactory
import com.tencent.devops.common.webhook.service.code.filter.UserFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils.convert
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory

@CodeWebhookHandler
@Suppress("TooManyFunctions")
class TGitGroupTriggerHandler : GitHookTriggerHandler<GitGroupEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitGroupTriggerHandler::class.java)
    }

    override fun eventClass(): Class<GitGroupEvent> {
        return GitGroupEvent::class.java
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.GROUP
    }

    override fun getUrl(event: GitGroupEvent): String {
        return event.group.homepage
    }

    override fun getUsername(event: GitGroupEvent): String {
        return event.user.username
    }

    override fun getAction(event: GitGroupEvent): String? {
        return event.operationKind
    }

    override fun getRevision(event: GitGroupEvent): String {
        return ""
    }

    override fun getRepoName(event: GitGroupEvent): String {
        return event.group.fullPath
    }

    override fun getBranchName(event: GitGroupEvent): String {
        return ""
    }

    override fun getMessage(event: GitGroupEvent): String {
        return event.group.name
    }

    override fun getEventDesc(event: GitGroupEvent): String {
        return I18Variable(
            code = getI18Code(event),
            params = listOf(
                getRepoUrl(event),
                event.objectAttributes.pathWithNamespace,
                getUsername(event),
                event.objectAttributes.oldPathWithNamespace ?: "",
                event.objectAttributes.pathWithNamespace
            )
        ).toJsonStr()
    }

    override fun getExternalId(event: GitGroupEvent): String {
        return event.group.groupId.toString()
    }

    override fun getEventFilters(
        event: GitGroupEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val userId = getUsername(event)
            val userFilter = UserFilter(
                pipelineId = pipelineId,
                triggerOnUser = getUsername(event),
                includedUsers = convert(includeUsers),
                excludedUsers = convert(excludeUsers),
                includedFailedReason = I18Variable(
                    code = WebhookI18nConstants.USER_NOT_MATCH,
                    params = listOf(userId)
                ).toJsonStr(),
                excludedFailedReason = I18Variable(
                    code = WebhookI18nConstants.USER_IGNORED,
                    params = listOf(userId)
                ).toJsonStr()
            )
            val actionFilter = ContainsFilter(
                pipelineId = pipelineId,
                filterName = "groupAction",
                triggerOn = getAction(event) ?: "",
                included = convert(includeRepoGroupAction).ifEmpty {
                    listOf("empty-action")
                },
                failedReason = I18Variable(
                    code = WebhookI18nConstants.GROUP_ACTION_NOT_MATCH,
                    params = listOf(getAction(event) ?: "")
                ).toJsonStr()
            )
            val repoFilter = object : WebhookFilter {
                override fun doFilter(response: WebhookFilterResponse): Boolean {
                    return PathFilterFactory.newPathFilter(
                        PathFilterConfig(
                            pathFilterType = PathFilterType.RegexBasedFilter,
                            pipelineId = pipelineId,
                            triggerOnPath = listOf(event.objectAttributes.pathWithNamespace),
                            includedPaths = convert(includeRepoNames),
                            excludedPaths = convert(excludeRepoNames),
                            includedFailedReason = I18Variable(
                                code = WebhookI18nConstants.REPO_NAME_NOT_MATCH,
                                params = listOf()
                            ).toJsonStr(),
                            excludedFailedReason = I18Variable(
                                code = WebhookI18nConstants.REPO_NAME_NOT_IGNORED,
                                params = listOf()
                            ).toJsonStr()
                        )
                    ).doFilter(response)
                }
            }
            return listOf(userFilter, actionFilter, repoFilter)
        }
    }

    override fun retrieveParams(
        event: GitGroupEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()

        return startParams
    }

    private fun getI18Code(event: GitGroupEvent) = with(event) {
        when (TGitProjectOperation.parse(operationKind)) {
            TGitProjectOperation.PROJECT_CREATE -> {
                WebhookI18nConstants.TGIT_PROJECT_CREATE_EVENT_DESC
            }

            TGitProjectOperation.PROJECT_DELETE -> {
                WebhookI18nConstants.TGIT_PROJECT_DELETE_EVENT_DESC
            }

            TGitProjectOperation.PROJECT_REFERENCE -> {
                WebhookI18nConstants.TGIT_PROJECT_REFERENCE_EVENT_DESC
            }

            TGitProjectOperation.PROJECT_DEREFERENCE -> {
                WebhookI18nConstants.TGIT_PROJECT_DEREFERENCE_EVENT_DESC
            }

            TGitProjectOperation.PROJECT_TRANSFER_IN -> {
                WebhookI18nConstants.TGIT_PROJECT_TRANSFER_IN_EVENT_DESC
            }

            TGitProjectOperation.PROJECT_TRANSFER_OUT -> {
                WebhookI18nConstants.TGIT_PROJECT_TRANSFER_OUT_EVENT_DESC
            }

            else -> {
                ""
            }
        }
    }

    private fun getRepoUrl(event: GitGroupEvent): String {
        val homepage = event.group.homepage
        return if (GitUtils.isValidGitUrl(homepage)) {
            val (domain, repoName) = GitUtils.getDomainAndRepoName(homepage)
            "https://$domain/${event.objectAttributes.pathWithNamespace}"
        } else {
            ""
        }
    }
}
