/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.websocket.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class WebSocketListener @Autowired constructor(
        val objectMapper: ObjectMapper,
        val messagingTemplate: SimpMessagingTemplate
): Listener<SendMessage> {

    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun execute(event: SendMessage) {
        logger.info("WebSocketListener: user:${event.userId},page:${event.page},notifyPost:${event.notifyPost},sessionList:${event.sessionList}")
        try {
            val sessionList = event.sessionList
            if (sessionList != null && sessionList.isNotEmpty()) {
                sessionList.forEach { session ->
                    messagingTemplate!!.convertAndSend(
                            "/topic/bk/notify/$session",
                            objectMapper.writeValueAsString(event.notifyPost))
                }
            }
        } catch (ex: Exception) {
            logger.error("webSocketListener error", ex)
        }
    }
}