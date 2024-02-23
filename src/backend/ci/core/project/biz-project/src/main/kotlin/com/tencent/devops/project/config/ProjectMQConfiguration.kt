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

package com.tencent.devops.project.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.project.listener.ProjectEventListener
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ProjectMQConfiguration {

    companion object {
        const val STREAM_CONSUMER_GROUP = "project-service"
    }

    @Bean
    fun sampleEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)

    @EventConsumer(StreamBinding.EXCHANGE_PROJECT_CREATE_FANOUT, STREAM_CONSUMER_GROUP)
    fun projectCreateEventListener(
        @Autowired listener: ProjectEventListener
    ): Consumer<Message<ProjectBroadCastEvent>> {
        return Consumer { event: Message<ProjectBroadCastEvent> ->
            listener.execute(event.payload)
        }
    }

    @EventConsumer(StreamBinding.EXCHANGE_PROJECT_UPDATE_FANOUT, STREAM_CONSUMER_GROUP)
    fun projectUpdateEventListener(
        @Autowired listener: ProjectEventListener
    ): Consumer<Message<ProjectBroadCastEvent>> {
        return Consumer { event: Message<ProjectBroadCastEvent> ->
            listener.execute(event.payload)
        }
    }

    @EventConsumer(StreamBinding.EXCHANGE_PROJECT_UPDATE_LOGO_FANOUT, STREAM_CONSUMER_GROUP)
    fun projectUpdateLogoEventListener(
        @Autowired listener: ProjectEventListener
    ): Consumer<Message<ProjectBroadCastEvent>> {
        return Consumer { event: Message<ProjectBroadCastEvent> ->
            listener.execute(event.payload)
        }
    }
}
