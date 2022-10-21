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

package com.tencent.devops.process.engine.init

import com.tencent.devops.common.event.annotation.StreamEventConsumer
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.engine.listener.run.monitor.PipelineBuildHeartbeatListener
import com.tencent.devops.process.engine.listener.run.monitor.PipelineBuildMonitorListener
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

/**
 * 流水线监控配置
 */
@Configuration
class BuildMonitorConfiguration {

    companion object {
        private const val STREAM_CONSUMER_GROUP = "engine-service"
    }

    /**
     * 监控队列--- 并发可小
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_MONITOR, STREAM_CONSUMER_GROUP)
    fun buildMonitorListener(
        @Autowired buildListener: PipelineBuildMonitorListener
    ): Consumer<Message<PipelineBuildMonitorEvent>> {
        return Consumer { event: Message<PipelineBuildMonitorEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * 心跳监听队列--- 并发可小
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_NOTIFY, STREAM_CONSUMER_GROUP)
    fun buildHeartBeatListener(
        @Autowired buildListener: PipelineBuildHeartbeatListener
    ): Consumer<Message<PipelineContainerAgentHeartBeatEvent>> {
        return Consumer { event: Message<PipelineContainerAgentHeartBeatEvent> ->
            buildListener.run(event.payload)
        }
    }
}
