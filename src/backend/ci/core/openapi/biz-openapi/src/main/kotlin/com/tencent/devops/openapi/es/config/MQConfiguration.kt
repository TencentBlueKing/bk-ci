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

package com.tencent.devops.openapi.es.config

import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.openapi.es.IESService
import com.tencent.devops.openapi.es.mq.ESEvent
import com.tencent.devops.openapi.es.mq.MQDispatcher
import com.tencent.devops.openapi.es.mq.MQListenerService
import java.util.function.Consumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.messaging.Message

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "elasticsearch")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class MQConfiguration @Autowired constructor() {

    @Bean
    fun openapiMQDispatcher(
        @Autowired streamBridge: StreamBridge
    ) = MQDispatcher(streamBridge)

    @Bean(StreamBinding.BINDING_OPENAPI_LOG_EVENT_IN)
    fun openapiLogEventIn(
        @Autowired listenerService: MQListenerService
    ): Consumer<Message<ESEvent>> {
        return Consumer { event: Message<ESEvent> ->
            listenerService.handleEvent(event.payload)
        }
    }

    @Bean
    fun mqListenerService(
        @Autowired logService: IESService,
        @Autowired dispatcher: MQDispatcher
    ) = MQListenerService(logService, dispatcher)
}
