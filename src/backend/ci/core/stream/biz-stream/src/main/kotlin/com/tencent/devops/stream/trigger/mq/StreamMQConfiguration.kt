/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
package com.tencent.devops.stream.trigger.mq

import com.tencent.devops.common.event.annotation.StreamEventConsumer
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQualityCheckBroadCastEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.stream.constant.MQ
import com.tencent.devops.stream.trigger.listener.StreamBuildFinishListenerService
import com.tencent.devops.stream.trigger.listener.StreamBuildQualityCheckListener
import com.tencent.devops.stream.trigger.mq.streamMrConflict.StreamMrConflictCheckEvent
import com.tencent.devops.stream.trigger.mq.streamMrConflict.StreamMrConflictCheckListener
import com.tencent.devops.stream.trigger.mq.streamRequest.StreamRequestEvent
import com.tencent.devops.stream.trigger.mq.streamRequest.StreamRequestListener
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerEvent
import com.tencent.devops.stream.trigger.mq.streamTrigger.StreamTriggerListener
import com.tencent.devops.stream.trigger.timer.listener.StreamTimerBuildListener
import com.tencent.devops.stream.trigger.timer.listener.StreamTimerChangerListener
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamChangeEvent
import com.tencent.devops.stream.trigger.timer.pojo.event.StreamTimerBuildEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
class StreamMQConfiguration {

    companion object {
        const val STREAM_CONSUMER_GROUP = "stream-service"
    }

    @Bean
    fun pipelineEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)

    @StreamEventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, STREAM_CONSUMER_GROUP)
    fun buildFinishListener(
        @Autowired finishListenerService: StreamBuildFinishListenerService
    ): Consumer<Message<PipelineBuildFinishBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildFinishBroadCastEvent> ->
            finishListenerService.doFinish(event.payload)
        }
    }

    @StreamEventConsumer(StreamBinding.EXCHANGE_PIPELINE_BUILD_QUALITY_CHECK_FANOUT, STREAM_CONSUMER_GROUP)
    fun buildQualityCheckListener(
        @Autowired checkListener: StreamBuildQualityCheckListener
    ): Consumer<Message<PipelineBuildQualityCheckBroadCastEvent>> {
        return Consumer { event: Message<PipelineBuildQualityCheckBroadCastEvent> ->
            checkListener.buildQualityCheckListener(event.payload)
        }
    }

    @StreamEventConsumer(MQ.QUEUE_STREAM_TRIGGER_PIPELINE_EVENT, STREAM_CONSUMER_GROUP)
    fun streamTriggerListener(
        @Autowired streamTriggerListener: StreamTriggerListener
    ): Consumer<Message<StreamTriggerEvent>> {
        return Consumer { event: Message<StreamTriggerEvent> ->
            streamTriggerListener.listenStreamTriggerEvent(event.payload)
        }
    }

    @StreamEventConsumer(MQ.QUEUE_STREAM_MR_CONFLICT_CHECK_EVENT, STREAM_CONSUMER_GROUP)
    fun conflictCheckListener(
        @Autowired checkListener: StreamMrConflictCheckListener
    ): Consumer<Message<StreamMrConflictCheckEvent>> {
        return Consumer { event: Message<StreamMrConflictCheckEvent> ->
            checkListener.listenGitCIRequestTriggerEvent(event.payload)
        }
    }

    @StreamEventConsumer(MQ.QUEUE_STREAM_REQUEST_EVENT, STREAM_CONSUMER_GROUP)
    fun streamRequestListener(
        @Autowired requestListener: StreamRequestListener
    ): Consumer<Message<StreamRequestEvent>> {
        return Consumer { event: Message<StreamRequestEvent> ->
            requestListener.listenStreamRequestEvent(event.payload)
        }
    }

    @StreamEventConsumer(MQ.QUEUE_STREAM_TIMER, STREAM_CONSUMER_GROUP)
    fun streamTimerBuildListener(
        @Autowired buildListener: StreamTimerBuildListener
    ): Consumer<Message<StreamTimerBuildEvent>> {
        return Consumer { event: Message<StreamTimerBuildEvent> ->
            buildListener.run(event.payload)
        }
    }

    // 每个实例都需要刷新自己维护的定时任务
    @StreamEventConsumer(MQ.QUEUE_STREAM_TIMER, STREAM_CONSUMER_GROUP, true)
    fun streamTimerChangerListener(
        @Autowired buildListener: StreamTimerChangerListener
    ): Consumer<Message<StreamChangeEvent>> {
        return Consumer { event: Message<StreamChangeEvent> ->
            buildListener.run(event.payload)
        }
    }
}
