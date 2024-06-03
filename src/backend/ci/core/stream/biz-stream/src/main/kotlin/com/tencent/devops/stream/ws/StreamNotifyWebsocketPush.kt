package com.tencent.devops.stream.ws

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.NotifyMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.WsRedisUtils

@Suppress("LongParameterList")
@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
class StreamNotifyWebsocketPush(
    val buildId: String?,
    val projectId: String?,
    override val userId: String,
    override val pushType: WebSocketType,
    override val redisOperation: RedisOperation,
    override var page: String?,
    override var notifyPost: NotifyPost
) : WebsocketPush(
    userId = userId,
    pushType = pushType,
    redisOperation = redisOperation,
    page = page,
    notifyPost = notifyPost
) {

    override fun findSession(page: String): Set<String> {
        return WsRedisUtils.getSessionIdByUserId(redisOperation, userId) ?: emptySet()
    }

    override fun buildMqMessage(): SendMessage {
        return NotifyMessage(
            buildId = null,
            pipelineId = "",
            projectId = projectId ?: "",
            userId = userId,
            sessionList = findSession(page ?: ""),
            page = page,
            notifyPost = notifyPost
        )
    }

    override fun buildNotifyMessage(message: SendMessage) = Unit
}
