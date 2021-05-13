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

package com.tencent.devops.process.engine.service.code.handler

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.process.engine.service.code.filter.EventTypeFilter
import com.tencent.devops.process.engine.service.code.filter.UrlFilter
import com.tencent.devops.process.engine.service.code.filter.UserFilter
import com.tencent.devops.process.engine.service.code.filter.WebhookFilter
import com.tencent.devops.process.pojo.code.ScmWebhookMatcher
import com.tencent.devops.process.pojo.code.WebHookEvent
import com.tencent.devops.repository.pojo.Repository

interface GitHookTriggerHandler : WebhookTriggerHandler {

    override fun getWebhookFilters(
        event: WebHookEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): List<WebhookFilter> {
        val filters = mutableListOf<WebhookFilter>()
        filters.addAll(
            initCommonFilters(
                event = event,
                pipelineId = pipelineId,
                repository = repository,
                webHookParams = webHookParams
            )
        )
        filters.addAll(
            getEventFilters(
                event = event,
                projectId = projectId,
                pipelineId = pipelineId,
                repository = repository,
                webHookParams = webHookParams
            )
        )
        return filters
    }

    fun getEventFilters(
        event: WebHookEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): List<WebhookFilter>

    fun getEventType(): CodeEventType

    fun getUrl(event: WebHookEvent): String

    fun getUser(event: WebHookEvent): String

    fun getAction(event: WebHookEvent): String? = null

    private fun initCommonFilters(
        event: WebHookEvent,
        pipelineId: String,
        repository: Repository,
        webHookParams: ScmWebhookMatcher.WebHookParams
    ): List<WebhookFilter> {
        with(webHookParams) {
            val urlFilter = UrlFilter(
                pipelineId = pipelineId,
                triggerOnUrl = getUrl(event),
                repositoryUrl = repository.url
            )
            val eventTypeFilter = EventTypeFilter(
                pipelineId = pipelineId,
                triggerOnEventType = getEventType(),
                eventType = eventType,
                action = getAction(event)
            )
            val userFilter = UserFilter(
                pipelineId = pipelineId,
                triggerOnUser = getUser(event),
                includedUsers = WebhookUtils.convert(includeUsers),
                excludedUsers = WebhookUtils.convert(excludeUsers)
            )
            return listOf(urlFilter, eventTypeFilter, userFilter)
        }
    }
}
