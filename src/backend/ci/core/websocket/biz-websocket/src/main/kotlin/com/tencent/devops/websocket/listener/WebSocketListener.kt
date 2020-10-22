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
import com.tencent.devops.common.websocket.dispatch.message.PipelineMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.websocket.servcie.WebsocketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Component
class WebSocketListener @Autowired constructor(
    val objectMapper: ObjectMapper,
    val messagingTemplate: SimpMessagingTemplate,
    val websocketService: WebsocketService
) : Listener<SendMessage> {

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun execute(event: SendMessage) {
        logger.debug("WebSocketListener: user:${event.userId},page:${event.page},sessionList:${event.sessionList}")
        try {
            val watch = StopWatch()
            val startTime = System.currentTimeMillis()

            if (isPushTimeOut(event)) {
                return
            }

            val sessionList = event.sessionList
            if (sessionList != null && sessionList.isNotEmpty()) {
                watch.start("addLongSession")
                addLongSession(sessionList, event.page ?: "")
                watch.stop()
                sessionList.forEach { session ->
                    if (websocketService.isCacheSession(session)) {
                        watch.start("PushMsg:$session")
                        val pushStartTime = System.currentTimeMillis()
                        messagingTemplate!!.convertAndSend(
                            "/topic/bk/notify/$session",
                            objectMapper.writeValueAsString(event.notifyPost)
                        )
                        watch.stop()
                        if (System.currentTimeMillis() - pushStartTime > 500) {
                            logger.warn("WebSocketListener push msg consuming 500ms, page[${event.page}, session[$session]")
                        }
                    }
                }
            } else {
                logger.info("webSocketListener sessionList is empty. page:${event.page} user:${event.userId} ")
            }
            if (System.currentTimeMillis() - startTime > 1000) {
                logger.warn("WebSocketListener push all message consuming 1s, page:[${event.page}] watch[$watch]")
            }
        } catch (ex: Exception) {
            logger.error("webSocketListener error", ex)
        }
    }

    private fun addLongSession(sessionList: List<String>, page: String) {
        if (sessionList.size < websocketService.getMaxSession()!!) {
            return
        }
        logger.warn("page[$page] sessionCount more ${websocketService.getMaxSession()}, sessionList[$sessionList]")
        websocketService.createLongSessionPage(page)
    }

    // 流水线消息默认2分钟推送超时，不做推送
    private fun isPushTimeOut(event: SendMessage): Boolean {
        if (event is PipelineMessage) {
            if (System.currentTimeMillis() - event.startTime > 2 * 60 * 1000) {
                logger.warn("websocket Consumers get message timeout | ${event.userId} | ${event.page} | ${event.buildId} | ${event.startTime}")
                return true
            }
        }
        return false
    }
}