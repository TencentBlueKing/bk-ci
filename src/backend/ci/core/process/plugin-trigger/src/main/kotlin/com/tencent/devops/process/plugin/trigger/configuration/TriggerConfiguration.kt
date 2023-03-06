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

package com.tencent.devops.process.plugin.trigger.configuration

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerChangeEvent
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.timer.SchedulerManager
import com.tencent.devops.process.plugin.trigger.timer.listener.PipelineTimerBuildListener
import com.tencent.devops.process.plugin.trigger.timer.listener.PipelineTimerChangerListener
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineJobBean
import com.tencent.devops.process.plugin.trigger.timer.quartz.QuartzSchedulerManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.quartz.QuartzProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

/**
 * @version 1.0
 */

@Configuration
@EnableConfigurationProperties(QuartzProperties::class)
@SuppressWarnings("TooManyFunctions")
class TriggerConfiguration {

    companion object {
        private const val STREAM_CONSUMER_GROUP = "process-service"
    }

    @Bean
    @SuppressWarnings("LongParameterList")
    fun pipelineJobBean(
        pipelineEventDispatcher: PipelineEventDispatcher,
        schedulerManager: SchedulerManager,
        pipelineTimerService: PipelineTimerService,
        redisOperation: RedisOperation,
        client: Client
    ): PipelineJobBean {
        return PipelineJobBean(
            pipelineEventDispatcher = pipelineEventDispatcher,
            schedulerManager = schedulerManager,
            pipelineTimerService = pipelineTimerService,
            redisOperation = redisOperation,
            client = client
        )
    }

    @Bean
    fun schedulerManager(quartzProperties: QuartzProperties) = QuartzSchedulerManager(quartzProperties)

    @Bean
    fun pipelineEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)

    /**
     * 定时构建队列--- 并发一般
     */
    @EventConsumer(StreamBinding.QUEUE_PIPELINE_TIMER, STREAM_CONSUMER_GROUP)
    fun timerTriggerListener(
        @Autowired buildListener: PipelineTimerBuildListener
    ): Consumer<Message<PipelineTimerBuildEvent>> {
        return Consumer { event: Message<PipelineTimerBuildEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * 构建定时构建定时变化的广播交换机
     */
    @EventConsumer(StreamBinding.EXCHANGE_PIPELINE_TIMER_CHANGE_FANOUT, STREAM_CONSUMER_GROUP, true)
    fun timerChangeListener(
        @Autowired buildListener: PipelineTimerChangerListener
    ): Consumer<Message<PipelineTimerChangeEvent>> {
        return Consumer { event: Message<PipelineTimerChangeEvent> ->
            buildListener.run(event.payload)
        }
    }
}
