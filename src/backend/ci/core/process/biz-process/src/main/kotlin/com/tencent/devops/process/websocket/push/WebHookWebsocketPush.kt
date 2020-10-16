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

package com.tencent.devops.process.websocket.push

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.NotifyMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.RedisUtlis

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class WebHookWebsocketPush(
    val buildId: String?,
    val pipelineId: String,
    val projectId: String,
    override val userId: String,
    override val pushType: WebSocketType,
    override val redisOperation: RedisOperation,
    override val objectMapper: ObjectMapper,
    override var page: String?,
    override var notifyPost: NotifyPost
) : WebsocketPush(userId, pushType, redisOperation, objectMapper, page, notifyPost) {

    override fun findSession(page: String): List<String>? {
        val sessions = RedisUtlis.getSessionIdByUserId(redisOperation, userId)
        val sessionList = sessions?.split(",")
        return if(sessionList?.size!! > 10) {
            // 为防止sessionId 太多,消息爆炸。截取最后十个session推送消息
            logger.warn("user open Page more 10, $userId | ${sessionList.size}| $sessionList")
            val lastSessions = mutableListOf<String>()
            for (index in sessionList.size downTo sessionList.size - 10) {
                lastSessions.add(sessionList[index])
            }
            lastSessions
        } else {
            sessionList
        }
    }

    override fun buildMqMessage(): SendMessage? {
        val sessionId = RedisUtlis.getSessionIdByUserId(redisOperation, userId)
        val sessionList = mutableListOf<String>()
        if (sessionId != null) {
            sessionList.add(sessionId!!)
        }
        return NotifyMessage(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                page = page,
                notifyPost = notifyPost,
                sessionList = sessionList
        )
    }

    override fun buildNotifyMessage(message: SendMessage) {
        val webhookMessage = message.notifyPost.message
//        logger.info("WebHookWebsocketPush message: $notifyPost")
    }
}