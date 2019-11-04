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
        return super.findSession(page)
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
        logger.info("WebHookWebsocketPush message: $notifyPost")
    }
}