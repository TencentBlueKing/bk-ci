/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.stream.binder.memory

import com.tencent.bkrepo.common.stream.binder.memory.queue.MemoryListenerContainer
import org.springframework.cloud.stream.provisioning.ConsumerDestination
import org.springframework.integration.endpoint.MessageProducerSupport
import org.springframework.messaging.Message
import java.util.UUID
import java.util.function.Consumer

/**
 * Memory integration endpoint
 */
class MemoryMessageProducer(
    private val destination: ConsumerDestination
) : MessageProducerSupport(), Consumer<Message<*>> {

    private val id = UUID.randomUUID()

    override fun accept(t: Message<*>) {
        this.sendMessage(t)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || MemoryMessageProducer::class.java != other::class.java) {
            return false
        }
        return this.id == (other as MemoryMessageProducer).id
    }

    override fun doStart() {
        MemoryListenerContainer.registerListener(destination.name, this)
    }

    override fun doStop() {
        MemoryListenerContainer.unregisterListener(destination.name)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
