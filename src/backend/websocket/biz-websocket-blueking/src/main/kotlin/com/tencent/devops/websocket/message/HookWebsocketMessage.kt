package com.tencent.devops.websocket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.MqMessage
import com.tencent.devops.common.websocket.dispatch.message.NotifyMqMessage
import com.tencent.devops.common.websocket.pojo.MessageInfo
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate

class HookWebsocketMessage @Autowired constructor(
    val messagingTemplate: SimpMessagingTemplate,
    val objectMapper: ObjectMapper,
    val redisOperation: RedisOperation
) : ISendMessage {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun sendWebsocketMessage(messageInfo: MessageInfo) {
        val result = objectMapper.writeValueAsString(messageInfo.notifyPost)
        val sessionId = RedisUtlis.getSessionIdByUserId(redisOperation, messageInfo.userId)
        if (sessionId != null) {
            if (sessionId.contains(","))
                sessionId.split(",").forEach {
                    messagingTemplate.convertAndSend("/topic/bk/notify/$it", result)
                    logger.info("PipelineWebSocketListener, WebSocketType:WEBHOOK, pipelineId: ${messageInfo.pipelineId},userId:${messageInfo.userId},sessionId:$it")
                }
            else {
                messagingTemplate.convertAndSend(
                    "/topic/bk/notify/$sessionId",
                    objectMapper.writeValueAsString(result)
                )
                logger.info("PipelineWebSocketListener, WebSocketType:WEBHOOK, pipelineId: ${messageInfo.pipelineId},userId:${messageInfo.userId},sessionId:$sessionId")
            }
        }
    }

    override fun buildMessageInfo(event: MqMessage): MessageInfo? {
        if (event is NotifyMqMessage) {
            return MessageInfo(
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                projectId = event.projectId,
                userId = event.userId,
                type = event.pushType,
                notifyPost = event.notifyPost,
                page = event.page!!
            )
        }
        return null
    }
}