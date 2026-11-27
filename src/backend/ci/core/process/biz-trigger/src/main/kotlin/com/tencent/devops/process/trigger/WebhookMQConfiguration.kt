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
