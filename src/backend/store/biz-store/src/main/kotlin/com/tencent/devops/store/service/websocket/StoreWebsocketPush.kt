package com.tencent.devops.store.service.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.AmdMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.store.service.atom.AtomReleaseService
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class StoreWebsocketPush(
    val buildId: String?,
    val pipelineId: String,
    val projectId: String,
    val atomId: String,
    val atomReleaseService: AtomReleaseService,
    override val userId: String,
    override val pushType: WebSocketType,
    override val redisOperation: RedisOperation,
    override val objectMapper: ObjectMapper,
    override var page: String?,
    override var notifyPost: NotifyPost
) : WebsocketPush(userId, pushType, redisOperation, objectMapper, page, notifyPost) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun findSession(page: String): List<String>? {
        return super.findSession(page)
    }

    override fun buildMqMessage(): SendMessage? {
        return AmdMessage(
                atomId = atomId,
                notifyPost = notifyPost,
                userId = userId,
                page = page,
                sessionList = findSession(page!!)!!
        )
    }

    override fun buildNotifyMessage(message: SendMessage) {
        val notifyPost = message.notifyPost
        try {
            val modelDetail = atomReleaseService.getProcessInfo(userId, atomId)
            if (notifyPost != null) {
                notifyPost.message = objectMapper.writeValueAsString(modelDetail)
                logger.info("StoreWebsocketPush message: $notifyPost")
            }
        } catch (e: Exception) {
            logger.error("DetailWebSocketMessage:getBuildDetail error. message:${e.message}")
        }
    }
}