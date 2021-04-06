/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.replication.handler

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.replication.config.NODE_REQUEST
import com.tencent.bkrepo.replication.message.node.NodeCopiedMessage
import com.tencent.bkrepo.replication.message.node.NodeCreatedMessage
import com.tencent.bkrepo.replication.message.node.NodeDeletedMessage
import com.tencent.bkrepo.replication.message.node.NodeMovedMessage
import com.tencent.bkrepo.replication.message.node.NodeRenamedMessage
import com.tencent.bkrepo.replication.message.node.NodeUpdatedMessage
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * consume event from caped collection and
 * dispatch it to event  handler
 */
@Component
class NodeEventConsumer(
    private val eventPublisher: ApplicationEventPublisher
) : AbstractHandler() {

    fun dealWithNodeCreateEvent(description: Map<String, Any>) {
        val request = description[NODE_REQUEST] as String
        JsonUtils.objectMapper.readValue(request, NodeCreateRequest::class.java).also {
            eventPublisher.publishEvent(NodeCreatedMessage(it))
        }
    }

    fun dealWithNodeRenameEvent(description: Map<String, Any>) {
        val request = description[NODE_REQUEST] as String
        JsonUtils.objectMapper.readValue(request, NodeRenameRequest::class.java).also {
            eventPublisher.publishEvent(NodeRenamedMessage(it))
        }
    }

    fun dealWithNodeCopyEvent(description: Map<String, Any>) {
        val request = description[NODE_REQUEST] as String
        JsonUtils.objectMapper.readValue(request, NodeCopyRequest::class.java).also {
            eventPublisher.publishEvent(NodeCopiedMessage(it))
        }
    }

    fun dealWithNodeDeleteEvent(description: Map<String, Any>) {
        val request = description[NODE_REQUEST] as String
        JsonUtils.objectMapper.readValue(request, NodeDeleteRequest::class.java).also {
            eventPublisher.publishEvent(NodeDeletedMessage(it))
        }
    }

    fun dealWithNodeMoveEvent(description: Map<String, Any>) {
        val request = description[NODE_REQUEST] as String
        JsonUtils.objectMapper.readValue(request, NodeMoveRequest::class.java).also {
            eventPublisher.publishEvent(NodeMovedMessage(it))
        }
    }

    fun dealWithNodeUpdateEvent(description: Map<String, Any>) {
        val request = description[NODE_REQUEST] as String
        JsonUtils.objectMapper.readValue(request, NodeUpdateRequest::class.java).also {
            eventPublisher.publishEvent(NodeUpdatedMessage(it))
        }
    }
}
