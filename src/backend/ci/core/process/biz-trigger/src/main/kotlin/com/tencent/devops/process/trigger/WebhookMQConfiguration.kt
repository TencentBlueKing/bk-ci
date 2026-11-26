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

package com.tencent.devops.process.trigger

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.trigger.artifact.ArtifactEventTriggerBuildService
import com.tencent.devops.process.trigger.artifact.ArtifactWebhookRequestService
import com.tencent.devops.process.trigger.event.ArtifactWebhookRequestEvent
import com.tencent.devops.process.trigger.event.ArtifactWebhookTriggerEvent
import com.tencent.devops.process.trigger.event.TapdWebhookRequestEvent
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import com.tencent.devops.process.trigger.tapd.TapdEventTriggerBuildService
import com.tencent.devops.process.trigger.tapd.TapdWebhookRequestService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * @ Date       ：Created in 16:50 2019-08-07
 */

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings("TooManyFunctions")
class WebhookMQConfiguration @Autowired constructor() {
    @EventConsumer(groupName = "process\${service-suffix}-tapdWebhookRequestEventConsumer")
    fun tapdWebhookRequestEventConsumer(
        @Autowired tapdWebhookRequestService: TapdWebhookRequestService
    ) = ScsConsumerBuilder.build<TapdWebhookRequestEvent> {
        tapdWebhookRequestService.handleRequest(it)
    }

    @EventConsumer(groupName = "process\${service-suffix}-tapdWebhookTriggerEventConsumer")
    fun tapdWebhookTriggerEventConsumer(
        @Autowired tapdEventTriggerBuildService: TapdEventTriggerBuildService
    ) = ScsConsumerBuilder.build<TapdWebhookTriggerEvent> {
        tapdEventTriggerBuildService.tapdWebhookTrigger(it)
    }

    @EventConsumer(groupName = "process\${service-suffix}-artifactWebhookRequestEventConsumer")
    fun artifactWebhookRequestEventConsumer(
        @Autowired artifactWebhookRequestService: ArtifactWebhookRequestService
    ) = ScsConsumerBuilder.build<ArtifactWebhookRequestEvent> {
        artifactWebhookRequestService.handleRequest(it)
    }

    @EventConsumer(groupName = "process\${service-suffix}-artifactWebhookTriggerEventConsumer")
    fun artifactWebhookTriggerEventConsumer(
        @Autowired artifactEventTriggerBuildService: ArtifactEventTriggerBuildService
    ) = ScsConsumerBuilder.build<ArtifactWebhookTriggerEvent> {
        artifactEventTriggerBuildService.artifactWebhookTrigger(it)
    }
}
