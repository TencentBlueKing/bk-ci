package com.tencent.devops.remotedev.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.remotedev.listener.RemoteDevUpdateListener
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class RemoteDevMQConfiguration {
    @EventConsumer
    fun remoteDevUpdateConsumer(
        @Autowired remoteDevUpdateListener: RemoteDevUpdateListener
    ) = ScsConsumerBuilder.build<RemoteDevUpdateEvent> { remoteDevUpdateListener.execute(it) }
}
