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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.stream.binder.memory

import com.tencent.bkrepo.common.stream.binder.memory.config.MemoryBinderConfigurationProperties
import com.tencent.bkrepo.common.stream.binder.memory.config.MemoryProducerProperties
import com.tencent.bkrepo.common.stream.binder.memory.queue.MemoryMessageQueue
import org.springframework.cloud.stream.binder.ExtendedProducerProperties
import org.springframework.cloud.stream.provisioning.ProducerDestination
import org.springframework.context.Lifecycle
import org.springframework.integration.handler.AbstractMessageHandler
import org.springframework.messaging.Message

class MemoryMessageHandler(
    private val configurationProperties: MemoryBinderConfigurationProperties,
    private val destination: ProducerDestination,
    private val producerProperties: ExtendedProducerProperties<MemoryProducerProperties>
) : AbstractMessageHandler(), Lifecycle {

    @Volatile
    private var started = false

    override fun handleMessageInternal(message: Message<*>?) {
        if (message != null) {
            MemoryMessageQueue.instance.produce(this.destination.name, message)
        }
    }

    override fun isRunning(): Boolean = started

    override fun start() {
        if (!started) {
            started = true
            MemoryMessageQueue.instance.start(
                configurationProperties.queueSize,
                configurationProperties.workerPoolSize
            )
        }
    }

    override fun stop() {
        if (started) {
            started = false
            MemoryMessageQueue.instance.shutdown()
        }
    }
}
