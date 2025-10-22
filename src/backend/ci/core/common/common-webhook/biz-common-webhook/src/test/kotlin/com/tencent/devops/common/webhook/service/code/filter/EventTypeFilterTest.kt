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

package com.tencent.devops.common.webhook.service.code.filter

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EventTypeFilterTest {
    private val response = WebhookFilterResponse()

    @Test
    fun isAllowedByEventType() {
        var eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.PUSH,
            eventType = null
        )
        Assertions.assertTrue(eventTypeFilter.doFilter(response))

        eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.PUSH,
            eventType = CodeEventType.PUSH
        )
        Assertions.assertTrue(eventTypeFilter.doFilter(response))

        eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.PUSH,
            eventType = CodeEventType.PULL_REQUEST
        )
        Assertions.assertFalse(eventTypeFilter.doFilter(response))
    }

    @Test
    fun isAllowedByMrAction() {
        var eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.PUSH,
            eventType = CodeEventType.MERGE_REQUEST
        )
        Assertions.assertFalse(eventTypeFilter.doFilter(response))

        eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.MERGE_REQUEST,
            eventType = CodeEventType.MERGE_REQUEST
        )
        Assertions.assertTrue(eventTypeFilter.doFilter(response))

        eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.MERGE_REQUEST,
            eventType = CodeEventType.MERGE_REQUEST
        )
        Assertions.assertTrue(eventTypeFilter.doFilter(response))

        eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.MERGE_REQUEST,
            eventType = CodeEventType.MERGE_REQUEST_ACCEPT
        )
        Assertions.assertTrue(eventTypeFilter.doFilter(response))

        eventTypeFilter = EventTypeFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            triggerOnEventType = CodeEventType.MERGE_REQUEST,
            eventType = CodeEventType.MERGE_REQUEST
        )
        Assertions.assertTrue(eventTypeFilter.doFilter(response))
    }
}
