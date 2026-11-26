package com.tencent.devops.process.plugin.trigger.configuration

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.process.trigger.event.PipelineTimerBuildEvent
import com.tencent.devops.process.trigger.timer.listener.PipelineTimerBuildListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * 定时触发执行配置，trigger 只负责加载定时任务并投递事件，由 process 消费执行
 */
@Configuration
class PipelineTimerBuildConfiguration {

    /**
     * 定时构建队列--- 并发一般
     */
    @EventConsumer
    fun timerTriggerConsumer(
        @Autowired buildListener: PipelineTimerBuildListener
    ) = ScsConsumerBuilder.build<PipelineTimerBuildEvent> { buildListener.run(it) }
}
