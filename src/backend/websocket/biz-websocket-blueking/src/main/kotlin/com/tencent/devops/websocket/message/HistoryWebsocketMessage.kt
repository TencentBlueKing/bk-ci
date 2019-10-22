package com.tencent.devops.websocket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.MqMessage
import com.tencent.devops.common.websocket.dispatch.message.PipelineMqMessage
import com.tencent.devops.common.websocket.pojo.MessageInfo
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class HistoryWebsocketMessage @Autowired constructor(
    val messagingTemplate: SimpMessagingTemplate,
    val redisOperation: RedisOperation,
    val objectMapper: ObjectMapper
) : ISendMessage {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun sendWebsocketMessage(messageInfo: MessageInfo) {
        logger.info("PipelineStatusChangeListener, changeType:HISTORY, pipelineId: ${messageInfo.pipelineId}, buildId: ${messageInfo.buildId}")
        val page = messageInfo.page
        val sessionList = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, page)
        buildMessage(messageInfo)
        if (sessionList != null) {
            sessionList.forEach {
                if (it == null) {
                    return@forEach
                }
                messagingTemplate!!.convertAndSend(
                    "/topic/bk/notify/$it",
                    objectMapper.writeValueAsString(messageInfo.notifyPost)
                )
                logger.info("HistoryWebSocketMessage-send topic to: /topic/bk/notify/$it, page:${messageInfo.page}")
            }
        }
        logger.info("send topic to: /topic/pipelineHistory/${messageInfo.pipelineId}")
    }

    override fun buildMessageInfo(event: MqMessage): MessageInfo? {
        if (event is PipelineMqMessage) {
            logger.info("[${event.buildId}] buildMessageInfo,event:$event")
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

    private fun buildMessage(messageInfo: MessageInfo) {
        val notifyPost = messageInfo.notifyPost
        val message = mutableMapOf<String, String>()
        try {
            if (notifyPost != null) {
                message.put("pipelineId", messageInfo.pipelineId!!)
                notifyPost.message = objectMapper.writeValueAsString(message)
            }
        } catch (e: Exception) {
            logger.error("HistoryWebSocketMessage:buildMessage error. message:${e.message}")
        }
    }
}