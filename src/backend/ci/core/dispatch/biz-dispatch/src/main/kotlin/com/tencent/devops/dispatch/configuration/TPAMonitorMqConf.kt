package com.tencent.devops.dispatch.configuration

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.dispatch.listener.TPAMonitorListener
import com.tencent.devops.dispatch.pojo.TPAMonitorEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class TPAMonitorMqConf {
    @EventConsumer
    fun tpaMonitorConsumer(
        @Autowired tpaMonitorListener: TPAMonitorListener
    ) = ScsConsumerBuilder.build<TPAMonitorEvent> { tpaMonitorListener.listenTPAMonitorEvent(it) }
}