package com.tencent.devops.dispatch.configuration

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.dispatch.listener.TPAQueueListener
import com.tencent.devops.dispatch.pojo.TPAQueueEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class TPAQueueMqConf {
    @EventConsumer
    fun asyncExecuteConsumer(
        @Autowired tpaQueueListener: TPAQueueListener
    ) = ScsConsumerBuilder.build<TPAQueueEvent> { tpaQueueListener.listenTpAgentQueueEvent(it) }
}