package com.tencent.devops.process.engine.init

import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.annotation.StreamEventConsumer
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.engine.listener.run.PipelineNotifyQueueListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
class PipelineNotifyQueueConfiguration {

    companion object {
        private const val STREAM_CONSUMER_GROUP = "process-service"
    }

    /**
     * webhook构建触发广播监听
     */
    @StreamEventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun notifyQueueBuildFinishListener(
        @Autowired buildListener: PipelineNotifyQueueListener
    ): Consumer<Message<PipelineBuildFinishBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildFinishBroadCastEvent> ->
            buildListener.run(event.payload)
        }
    }
}
