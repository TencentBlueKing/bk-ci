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

package com.tencent.devops.remotedev.websocket.push

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.WsRedisUtils
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import io.swagger.annotations.ApiModelProperty
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class WorkspaceWebsocketPush(
    val type: WebSocketActionType,
    @ApiModelProperty("行为是否成功")
    val status: Boolean,
    val anyMessage: Any,
    val projectId: String,
    val userIds: Set<String>,
    override val redisOperation: RedisOperation,
    override var page: String?,
    override var notifyPost: NotifyPost
) : WebsocketPush(
    userId = userIds.firstOrNull() ?: "",
    pushType = WebSocketType.AMD,
    redisOperation = redisOperation,
    page = page,
    notifyPost = notifyPost
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceWebsocketPush::class.java)
    }

    // 同时向 user 和 page 的 session 推送。可以同时兼容概览页面和详情页面
    override fun findSession(page: String): Set<String>? {
        val userSession = mutableSetOf<String>()
        userIds.forEach {
            userSession.addAll(
                WsRedisUtils.getSessionIdByUserId(redisOperation, it) ?: emptySet()
            )
        }
        val pageSession = super.findSession(page) ?: emptySet()
        return userSession.plus(pageSession)
    }

    override fun buildMqMessage(): SendMessage {
        return SendMessage(
            notifyPost = notifyPost,
            userId = userId,
            page = page,
            sessionList = findSession(page!!)
        )
    }

    override fun buildNotifyMessage(message: SendMessage) {
        val messageMap = mutableMapOf<String, Any>("type" to type, "status" to status)
        messageMap["data"] = anyMessage
        try {
            message.notifyPost.message = JsonUtil.toJson(messageMap, formatted = false)
        } catch (ignored: Exception) {
            logger.error("HistoryWebSocketMessage:buildMessage error. message:${ignored.message}")
        }
    }
}
