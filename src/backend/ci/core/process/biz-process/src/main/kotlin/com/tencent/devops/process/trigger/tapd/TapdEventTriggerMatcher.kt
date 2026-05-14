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

package com.tencent.devops.process.trigger.tapd

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerInput
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TRIGGER_ACTION_NOT_MATCH
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.USER_IGNORED
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.USER_NOT_MATCH
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.UserFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.pojo.WebhookAtomResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TAPD 触发器匹配器
 */
@Service
class TapdEventTriggerMatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(TapdEventTriggerMatcher::class.java)
    }

    fun matches(
        element: TapdWebHookTriggerElement,
        tapdProjectId: String,
        eventType: TapdEventType,
        eventAction: TapdEventAction,
        triggerUser: String
    ): WebhookAtomResponse {
        val input = element.data.input
        val taskId = element.id ?: ""
        // 1. 项目过滤
        if (input.tapdProjectId.isBlank() || input.tapdProjectId != tapdProjectId) {
            logger.info(
                "tapd trigger project mismatch|element=${input.tapdProjectId}|webhook=$tapdProjectId"
            )
            return WebhookAtomResponse(MatchStatus.REPOSITORY_NOT_MATCH)
        }
        // 2. 事件类型过滤
        if (input.eventType != eventType) {
            logger.info(
                "tapd trigger event type mismatch|element=${input.eventType}|webhook=${eventType.value}"
            )
            return WebhookAtomResponse(matchStatus = MatchStatus.EVENT_TYPE_NOT_MATCH)
        }
        // 3. 其他过滤条件
        getEventFilters(
            input = input,
            taskId = taskId,
            eventAction = eventAction,
            triggerUser = triggerUser
        ).forEach {
            val filterResponse = WebhookFilterResponse()
            if (!it.doFilter(filterResponse)) {
                return WebhookAtomResponse(
                    matchStatus = MatchStatus.CONDITION_NOT_MATCH,
                    failedReason = filterResponse.failedReason
                )
            }
        }
        return WebhookAtomResponse(matchStatus = MatchStatus.SUCCESS)
    }

    private fun getEventFilters(
        input: TapdWebHookTriggerInput,
        taskId: String,
        eventAction: TapdEventAction,
        triggerUser: String
    ): List<WebhookFilter> {
        val actionFilter = ContainsFilter(
            pipelineId = taskId,
            filterName = "tapdAction",
            triggerOn = eventAction.value,
            included = when (input.eventType) {
                TapdEventType.STORY -> input.includeStoryAction
                TapdEventType.BUG -> input.includeStoryAction
                else -> listOf()
            }?.filter { it.isNotBlank() } ?: emptyList(),
            failedReason = I18Variable(
                code = BK_TRIGGER_ACTION_NOT_MATCH,
                params = listOf()
            ).toJsonStr()
        )
        val userFilter = UserFilter(
            pipelineId = taskId,
            triggerOnUser = triggerUser,
            includedUsers = input.includeUsers?.filter { it.isNotBlank() } ?: emptyList(),
            excludedUsers = input.excludeUsers?.filter { it.isNotBlank() } ?: emptyList(),
            includedFailedReason = I18Variable(
                code = USER_NOT_MATCH,
                params = listOf(triggerUser)
            ).toJsonStr(),
            excludedFailedReason = I18Variable(
                code = USER_IGNORED,
                params = listOf(triggerUser)
            ).toJsonStr()
        )
        return listOf(actionFilter, userFilter)
    }
}
