package com.tencent.devops.process.callback.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.callback.listener.ProjectCallbackEventListener
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectEnableStatusBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ProjectCallbackMQConfiguration {
    @EventConsumer
    fun projectCreateListener(
        @Autowired listener: ProjectCallbackEventListener
    ) = ScsConsumerBuilder.build<ProjectCreateBroadCastEvent> { listener.execute(it) }

    @EventConsumer
    fun projectUpdateListener(
        @Autowired listener: ProjectCallbackEventListener
    ) = ScsConsumerBuilder.build<ProjectUpdateBroadCastEvent> { listener.execute(it) }

    @EventConsumer
    fun projectEnableListener(
        @Autowired listener: ProjectCallbackEventListener
    ) = ScsConsumerBuilder.build<ProjectEnableStatusBroadCastEvent> { listener.execute(it) }
}
