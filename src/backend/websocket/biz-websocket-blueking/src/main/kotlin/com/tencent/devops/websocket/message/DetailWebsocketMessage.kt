package com.tencent.devops.websocket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
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
class DetailWebsocketMessage @Autowired constructor(
    val messagingTemplate: SimpMessagingTemplate,
    val objectMapper: ObjectMapper,
    val client: Client,
    val redisOperation: RedisOperation
) : ISendMessage {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun sendWebsocketMessage(messageInfo: MessageInfo) {
        logger.info("PipelineStatusChangeListener, changeType:DETAIL, pipelineId: ${messageInfo.pipelineId}, buildId: ${messageInfo.buildId}")
        if (messageInfo.buildId != null) {
            buildMessage(messageInfo)
            val page = messageInfo.page
            val sessionList = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, page)
            if (sessionList != null && sessionList.size > 0) {
                sessionList.forEach {
                    if (it == null) {
                        return@forEach
                    }
                    if (!messageInfo.notifyPost!!.message.isNullOrEmpty()) {
                        messagingTemplate!!.convertAndSend(
                            "/topic/bk/notify/$it",
                            objectMapper.writeValueAsString(messageInfo.notifyPost)
                        )
                        logger.info("DetailWebSocketMessage-send topic to: /topic/bk/notify/$it, page:${messageInfo.page}")
                    }
                }
            }
        }
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
        try {
//            val modelDetail = client.get(BuildBuildResource::class)
//                .getBuildDetail(
//                    messageInfo.projectId!!,
//                    messageInfo.pipelineId!!,
//                    messageInfo.buildId!!,
//                    ChannelCode.BS
//                )
//                .data
            val modelDetail = null
            if (notifyPost != null) {
                notifyPost.message = objectMapper.writeValueAsString(modelDetail)
            }
        } catch (e: Exception) {
            logger.error("DetailWebSocketMessage:getBuildDetail error. message:${e.message}")
        }
    }
}