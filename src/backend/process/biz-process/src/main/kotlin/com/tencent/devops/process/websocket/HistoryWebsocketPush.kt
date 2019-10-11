package com.tencent.devops.process.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.message.PipelineMessage
import com.tencent.devops.common.websocket.dispatch.message.PipelineMqMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.dispatch.push.IWebsocketPush
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class HistoryWebsocketPush(
        val buildId: String?,
        val pipelineId: String,
        val projectId: String,
        override val userId: String,
        override val pathClass: IPath,
        override val pushType: WebSocketType,
        override val redisOperation: RedisOperation,
        override val objectMapper: ObjectMapper,
        override var page: String?,
        override var notifyPost: NotifyPost
) : WebsocketPush(userId, pathClass, pushType, redisOperation, objectMapper, page, notifyPost) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun findSession(page: String): List<String>? {
        return super.findSession(page)
    }

    override fun buildMqMessage(): SendMessage? {
        return PipelineMessage(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                notifyPost = notifyPost,
                userId = userId,
                page = page,
                sessionList = findSession(page!!)!!
        )
    }

    override fun buildNotifyMessage(message: SendMessage) {
        val notifyPost = message.notifyPost
        val message = mutableMapOf<String, String>()
        try {
            if (notifyPost != null) {
                message.put("pipelineId", pipelineId!!)
                notifyPost.message = objectMapper.writeValueAsString(message)
            }
        } catch (e: Exception) {
            logger.error("HistoryWebSocketMessage:buildMessage error. message:${e.message}")
        }
    }
}