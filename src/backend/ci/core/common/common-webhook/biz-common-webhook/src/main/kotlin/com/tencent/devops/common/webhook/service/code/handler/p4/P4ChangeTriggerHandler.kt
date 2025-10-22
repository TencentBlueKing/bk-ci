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
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_P4_WEBHOOK_CHANGE
import com.tencent.devops.common.webhook.pojo.code.PathFilterConfig
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.p4.P4ChangeEvent
import com.tencent.devops.common.webhook.service.code.EventCacheService
import com.tencent.devops.common.webhook.service.code.filter.EventTypeFilter
import com.tencent.devops.common.webhook.service.code.filter.P4PortFilter
import com.tencent.devops.common.webhook.service.code.filter.PathFilterFactory
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.handler.CodeWebhookTriggerHandler
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.repository.pojo.Repository

@CodeWebhookHandler
@SuppressWarnings("TooManyFunctions")
class P4ChangeTriggerHandler(
    private val eventCacheService: EventCacheService
) : CodeWebhookTriggerHandler<P4ChangeEvent> {
    override fun eventClass(): Class<P4ChangeEvent> {
        return P4ChangeEvent::class.java
    }

    override fun getUrl(event: P4ChangeEvent) = event.p4Port

    override fun getUsername(event: P4ChangeEvent) = event.user ?: ""

    override fun getRevision(event: P4ChangeEvent) = event.change.toString()

    override fun getRepoName(event: P4ChangeEvent) = event.p4Port

    override fun getBranchName(event: P4ChangeEvent) = ""

    @Deprecated(
        message = "p4 use getEventType(event: P4ChangeEvent)",
        replaceWith = ReplaceWith("@see getEventType(event)")
    )
    override fun getEventType(): CodeEventType = CodeEventType.CHANGE_COMMIT

    override fun getEventType(event: P4ChangeEvent): CodeEventType = when (event.eventType) {
        P4ChangeEvent.CHANGE_COMMIT -> CodeEventType.CHANGE_COMMIT
        P4ChangeEvent.CHANGE_SUBMIT -> CodeEventType.CHANGE_SUBMIT
        P4ChangeEvent.CHANGE_CONTENT -> CodeEventType.CHANGE_SUBMIT
        else ->
            CodeEventType.valueOf(event.eventType)
    }

    override fun getMessage(event: P4ChangeEvent) = event.description

    override fun getEventDesc(event: P4ChangeEvent): String {
        return I18Variable(
            code = WebhookI18nConstants.P4_EVENT_DESC,
            params = listOf(
                getRevision(event),
                getUsername(event),
                getFormatEventType(event)
            )
        ).toJsonStr()
    }

    override fun getExternalId(event: P4ChangeEvent): String {
        return event.p4Port
    }

    override fun getWebhookFilters(
        event: P4ChangeEvent,
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
                    // 用户配置的脚本触发,变更文件由触发脚本解析
                    val changeFiles =
                        if (WebhookUtils.isCustomP4TriggerVersion(webHookParams.version)) {
                            caseSensitive = event.caseSensitive ?: true
                            event.files ?: emptyList()
                        } else {
                            val p4ServerInfo = eventCacheService.getP4ServerInfo(
                                repo = repository,
                                projectId = projectId,
                                repositoryId = repositoryConfig.getURLEncodeRepositoryId(),
                                repositoryType = repositoryConfig.repositoryType
                            )
                            p4ServerInfo?.run {
                                caseSensitive = this.caseSensitive
                            }
                            eventCacheService.getP4ChangelistFiles(
                                repo = repository,
                                projectId = projectId,
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
        event: P4ChangeEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_P4_WEBHOOK_CHANGE] = event.change
        startParams[PIPELINE_BUILD_MSG] = event.description ?: P4ChangeEvent.DEFAULT_CHANGE_DESCRIPTION
        return startParams
    }

    private fun getFormatEventType(event: P4ChangeEvent) = when (event.eventType) {
        CodeEventType.CHANGE_COMMIT.name -> P4ChangeEvent.CHANGE_COMMIT
        CodeEventType.CHANGE_SUBMIT.name -> P4ChangeEvent.CHANGE_SUBMIT
        CodeEventType.CHANGE_CONTENT.name -> P4ChangeEvent.CHANGE_CONTENT
        else -> event.eventType
    }
}
