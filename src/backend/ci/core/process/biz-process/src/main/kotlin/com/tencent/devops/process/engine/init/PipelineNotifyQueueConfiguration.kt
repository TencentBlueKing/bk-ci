package com.tencent.devops.process.engine.init

import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.engine.listener.run.PipelineNotifyQueueListener
import com.tencent.devops.process.engine.service.PipelineNotifyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
class PipelineNotifyQueueConfiguration {

    companion object {
        private const val STREAM_CONSUMER_GROUP = "process-service"
    }

    @Bean
    fun pipelineNotifyQueueListener(
        @Autowired pipelineNotifyService: PipelineNotifyService,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher
    ) = PipelineNotifyQueueListener(pipelineNotifyService, pipelineEventDispatcher)

    /**
     * webhook构建触发广播监听
     */
    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun notifyQueueBuildFinishListener(
        @Autowired buildListener: PipelineNotifyQueueListener
    ): Consumer<Message<PipelineBuildFinishBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildFinishBroadCastEvent> ->
            buildListener.run(event.payload)
        }
    }
}
