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

package com.tencent.bkrepo.repository.listener

import com.tencent.bkrepo.common.stream.message.node.NodeCopiedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeCreatedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeDeletedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeMovedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeRenamedMessage
import com.tencent.bkrepo.common.stream.message.node.NodeUpdatedMessage
import com.tencent.bkrepo.repository.listener.event.node.NodeCopiedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeCreatedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeDeletedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeMovedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeRenamedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeUpdatedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NodeEventListener : AbstractEventListener() {

    @Async
    @EventListener(NodeCreatedEvent::class)
    fun handle(event: NodeCreatedEvent) {
        event.apply { sendMessage(NodeCreatedMessage(request)) }.also { logEvent(it) }
    }

    @Async
    @EventListener(NodeRenamedEvent::class)
    fun handle(event: NodeRenamedEvent) {
        event.apply { sendMessage(NodeRenamedMessage(request)) }.also { logEvent(it) }
    }

    @Async
    @EventListener(NodeUpdatedEvent::class)
    fun handle(event: NodeUpdatedEvent) {
        event.apply { sendMessage(NodeUpdatedMessage(request)) }.also { logEvent(it) }
    }

    @Async
    @EventListener(NodeMovedEvent::class)
    fun handle(event: NodeMovedEvent) {
        event.apply { sendMessage(NodeMovedMessage(request)) }.also { logEvent(it) }
    }

    @Async
    @EventListener(NodeCopiedEvent::class)
    fun handle(event: NodeCopiedEvent) {
        event.apply { sendMessage(NodeCopiedMessage(request)) }.also { logEvent(it) }
    }

    @Async
    @EventListener(NodeDeletedEvent::class)
    fun handle(event: NodeDeletedEvent) {
        event.apply { sendMessage(NodeDeletedMessage(request)) }.also { logEvent(it) }
    }
}
