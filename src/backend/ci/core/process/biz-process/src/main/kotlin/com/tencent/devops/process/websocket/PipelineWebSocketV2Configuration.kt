package com.tencent.devops.process.websocket

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.common.websocket.v2.WsInvalidation
import com.tencent.devops.common.websocket.v2.WsInvalidationPublisher
import com.tencent.devops.common.websocket.v2.WsTopic
import com.tencent.devops.common.websocket.v2.WsV2RabbitConfiguration
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(WsV2RabbitConfiguration::class)
class PipelineWebSocketV2Configuration {
    @EventConsumer(defaultConcurrency = 2)
    fun pipelineWebsocketV2InvalidationConsumer(publisher: WsInvalidationPublisher) =
        ScsConsumerBuilder.build<PipelineBuildWebSocketPushEvent> { event ->
            topics(event).forEach { publisher.publish(WsInvalidation(it)) }
        }

    companion object {
        fun topics(event: PipelineBuildWebSocketPushEvent): Set<String> {
            val topics = linkedSetOf<String>()
            if (event.refreshTypes and 2L != 0L) topics.add(WsTopic.history(event.projectId, event.pipelineId))
            if (event.refreshTypes and 8L != 0L) topics.add(WsTopic.status(event.projectId, event.pipelineId))
            if (event.refreshTypes and 16L != 0L) {
                topics.add(WsTopic.record(event.projectId, event.pipelineId, event.buildId))
                val executeCount = event.executeCount
                if (executeCount != null && executeCount > 0) {
                    topics.add(WsTopic.record(event.projectId, event.pipelineId, event.buildId, executeCount))
                } else {
                    topics.add(WsTopic.recordScope(event.projectId, event.pipelineId, event.buildId))
                }
            }
            return topics
        }
    }
}
