package com.tencent.devops.common.websocket.dispatch.push

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.message.NotifyMqMessage
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
class NotifyWebsocketPush(
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
) : IWebsocketPush(userId, pathClass, pushType, redisOperation, objectMapper, page, notifyPost) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    // notify只需要user有对应session就满足推送条件，无需session匹配到page
    override fun isPushBySession(): Boolean {
        val sessionIdList = RedisUtlis.getSessionIdByUserId(redisOperation, userId)
        logger.info("NotifyWebsocketPush: userId:$userId, sessionList:$sessionIdList")
        if (sessionIdList != null) {
            return true
        }
        return false
    }

    override fun isPushByPage(): Boolean {
        return true
    }

    override fun mqMessage(): NotifyMqMessage {
        val mqMessage = NotifyMqMessage(
            userId = userId,
            page = page,
            notifyPost = notifyPost,
            pushType = pushType,
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId
        )
        return mqMessage
    }

    override fun buildMessage(messageInfo: IWebsocketPush) {
        return
    }
}