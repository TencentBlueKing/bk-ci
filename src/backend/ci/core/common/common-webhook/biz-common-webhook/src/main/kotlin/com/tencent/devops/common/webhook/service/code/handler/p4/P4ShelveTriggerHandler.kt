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

package com.tencent.devops.common.webhook.service.code.handler.p4

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_P4_WEBHOOK_CHANGE
import com.tencent.devops.common.webhook.pojo.code.PathFilterConfig
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.p4.P4ShelveEvent
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.P4PortFilter
import com.tencent.devops.common.webhook.service.code.filter.PathFilterFactory
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.repository.api.ServiceP4Resource
import com.tencent.devops.repository.pojo.Repository

@CodeWebhookHandler
@SuppressWarnings("TooManyFunctions")
class P4ShelveTriggerHandler(
    private val client: Client,
    private val eventCacheService: EventCacheService
) : CodeWebhookTriggerHandler<P4ShelveEvent> {
    override fun eventClass(): Class<P4ShelveEvent> {
        return P4ShelveEvent::class.java
    }

    override fun getUrl(event: P4ShelveEvent) = event.p4Port

    override fun getUsername(event: P4ShelveEvent) = event.user ?: ""

    override fun getRevision(event: P4ShelveEvent) = event.change.toString()

    override fun getRepoName(event: P4ShelveEvent) = event.p4Port

    override fun getBranchName(event: P4ShelveEvent) = ""

    @Deprecated(
        message = "p4 use getEventType(event: P4ShelveEvent)",
        replaceWith = ReplaceWith("@see getEventType(event)")
    )
    override fun getEventType(): CodeEventType = CodeEventType.SHELVE_COMMIT

    override fun getEventType(event: P4ShelveEvent): CodeEventType = when (event.eventType) {
        P4ShelveEvent.SHELVE_COMMIT -> CodeEventType.SHELVE_COMMIT
        P4ShelveEvent.SHELVE_DELETE -> CodeEventType.SHELVE_DELETE
        P4ShelveEvent.SHELVE_SUBMIT -> CodeEventType.SHELVE_SUBMIT
        else ->
            CodeEventType.valueOf(event.eventType)
    }

    override fun getMessage(event: P4ShelveEvent) = event.description

    override fun getEventDesc(event: P4ShelveEvent): String {
        return I18Variable(
            code = WebhookI18nConstants.P4_EVENT_DESC,
            params = listOf(
                getRevision(event),
                getUsername(event),
                getFormatEventType(event)
            )
        ).toJsonStr()
    }

    override fun getExternalId(event: P4ShelveEvent): String {
        return event.p4Port
    }

    override fun getWebhookFilters(
        event: P4ShelveEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val p4Filter = WebhookUtils.getP4Filter(
                projectId = projectId,
                pipelineId = pipelineId,
                event = event,
                webHookParams = webHookParams
            )
            val urlFilter = P4PortFilter(
                pipelineId = pipelineId,
                triggerOnP4port = event.p4Port,
                repositoryP4Port = repository.url
            )
            val eventTypeFilter = EventTypeFilter(
                pipelineId = pipelineId,
                triggerOnEventType = getEventType(event),
                eventType = webHookParams.eventType
            )
            val pathFilter = object : WebhookFilter {
                override fun doFilter(response: WebhookFilterResponse): Boolean {
                    if (includePaths.isNullOrBlank() && excludePaths.isNullOrBlank()) {
                        return true
                    }
                    // 默认区分大小写
                    var caseSensitive = true
                    val changeFiles = if (WebhookUtils.isCustomP4TriggerVersion(webHookParams.version)) {
                        caseSensitive = event.caseSensitive ?: true
                        event.files ?: emptyList()
                    } else {
                        val p4ServerInfo = client.get(ServiceP4Resource::class).getServerInfo(
                            projectId = projectId,
                            repositoryId = repositoryConfig.getURLEncodeRepositoryId(),
                            repositoryType = repositoryConfig.repositoryType
                        )
                        p4ServerInfo.data?.run {
                            caseSensitive = this.caseSensitive
                        }

                        eventCacheService.getP4ShelvedChangelistFiles(
                            projectId = projectId,
                            repo = repository,
                            repositoryId = repositoryConfig.getURLEncodeRepositoryId(),
                            repositoryType = repositoryConfig.repositoryType,
                            change = event.change
                        )?.run {
                            event.description = this.description
                            this.fileList.map { it.depotPathString }
                        } ?: emptyList()
                    }
                    return PathFilterFactory.newPathFilter(
                        PathFilterConfig(
                            pathFilterType = PathFilterType.RegexBasedFilter,
                            pipelineId = pipelineId,
                            triggerOnPath = changeFiles,
                            includedPaths = WebhookUtils.convert(includePaths),
                            excludedPaths = WebhookUtils.convert(excludePaths),
                            caseSensitive = caseSensitive,
                            includedFailedReason = I18Variable(
                                code = WebhookI18nConstants.PATH_NOT_MATCH,
                                params = listOf()
                            ).toJsonStr(),
                            excludedFailedReason = I18Variable(
                                code = WebhookI18nConstants.PATH_IGNORED,
                                params = listOf()
                            ).toJsonStr()
                        )
                    ).doFilter(response)
                }
            }
            return listOf(p4Filter, urlFilter, eventTypeFilter, pathFilter)
        }
    }

    override fun retrieveParams(
        event: P4ShelveEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_P4_WEBHOOK_CHANGE] = event.change
        startParams[PIPELINE_BUILD_MSG] = event.description ?: P4ShelveEvent.DEFAULT_SHELVE_DESCRIPTION
        return startParams
    }

    private fun getFormatEventType(event: P4ShelveEvent) = when (event.eventType) {
        CodeEventType.SHELVE_COMMIT.name -> P4ShelveEvent.SHELVE_COMMIT
        CodeEventType.SHELVE_SUBMIT.name -> P4ShelveEvent.SHELVE_SUBMIT
        CodeEventType.SHELVE_DELETE.name -> P4ShelveEvent.SHELVE_DELETE
        else -> event.eventType
    }
}
