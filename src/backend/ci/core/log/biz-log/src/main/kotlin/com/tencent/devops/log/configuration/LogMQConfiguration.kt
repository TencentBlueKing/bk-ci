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

package com.tencent.devops.log.configuration

import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.log.event.LogOriginEvent
import com.tencent.devops.log.event.LogStatusEvent
import com.tencent.devops.log.event.LogStorageEvent
import com.tencent.devops.log.jmx.LogPrintBean
import com.tencent.devops.log.service.BuildLogListenerService
import com.tencent.devops.log.service.BuildLogPrintService
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
class LogMQConfiguration {

    @Bean
    fun buildLogPrintService(
        streamBridge: StreamBridge,
        logPrintBean: LogPrintBean,
        storageProperties: StorageProperties,
        logServiceConfig: LogServiceConfig
    ) = BuildLogPrintService(streamBridge, logPrintBean, storageProperties, logServiceConfig)

    @Bean(StreamBinding.BINDING_LOG_ORIGIN_EVENT_IN)
    fun logOriginEventIn(
        listenerService: BuildLogListenerService
    ): Consumer<Message<LogOriginEvent>> {
        return Consumer { event: Message<LogOriginEvent> ->
            listenerService.handleEvent(event.payload)
        }
    }

    @Bean(StreamBinding.BINDING_LOG_STORAGE_EVENT_IN)
    fun logStorageEventIn(
        listenerService: BuildLogListenerService
    ): Consumer<Message<LogStorageEvent>> {
        return Consumer { event: Message<LogStorageEvent> ->
            listenerService.handleEvent(event.payload)
        }
    }

    @Bean(StreamBinding.BINDING_LOG_STATUS_EVENT_IN)
    fun logStatusEventIn(
        listenerService: BuildLogListenerService
    ): Consumer<Message<LogStatusEvent>> {
        return Consumer { event: Message<LogStatusEvent> ->
            listenerService.handleEvent(event.payload)
        }
    }
}
