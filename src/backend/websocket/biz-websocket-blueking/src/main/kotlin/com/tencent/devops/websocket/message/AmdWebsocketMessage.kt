package com.tencent.devops.websocket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.AmdMqMessage
import com.tencent.devops.common.websocket.dispatch.message.MqMessage
import com.tencent.devops.common.websocket.pojo.MessageInfo
import com.tencent.devops.common.websocket.utils.PageUtils
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.store.api.atom.UserMarketAtomResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class AmdWebsocketMessage @Autowired constructor(
    val messagingTemplate: SimpMessagingTemplate,
    val redisOperation: RedisOperation,
    val objectMapper: ObjectMapper,
    val client: Client
) : ISendMessage {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun sendWebsocketMessage(messageInfo: MessageInfo) {
        logger.info("AmdWebSocketMessage, changeType:Store, pipelineId: ${messageInfo.pipelineId}, buildId: ${messageInfo.buildId}")
        if (messageInfo.atomId != null) {
            buildMessage(messageInfo)
            val page = messageInfo.page
            val pageList = createAssociationPage(page)
            if (pageList != null && pageList.size > 0) {
                pageList.forEach { it ->
                    val sessionList = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, it)
                    if (sessionList != null && sessionList.size > 0) {
                        sessionList.forEach { session ->
                            if (session == null) {
                                return@forEach
                            }

                            if (!messageInfo.notifyPost!!.message.isNullOrEmpty()) {
                                messagingTemplate!!.convertAndSend(
                                    "/topic/bk/notify/$session",
                                    objectMapper.writeValueAsString(messageInfo.notifyPost)
                                )
                                logger.info("AmdWebSocketMessage-send topic to: /topic/bk/notify/$session, page:${messageInfo.page}")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun buildMessageInfo(event: MqMessage): MessageInfo? {
        if (event is AmdMqMessage) {
            return MessageInfo(
                pipelineId = null,
                buildId = null,
                projectId = null,
                atomId = event.atomId,
                userId = event.userId,
                type = event.pushType,
                notifyPost = event.notifyPost,
                page = event.page!!
            )
        }
        return null
    }

    private fun createAssociationPage(page: String): List<String> {
        val pageList = mutableListOf<String>()
        pageList.add(page)
        val associationPage = PageUtils.replaceAssociationPage(page)
        if (associationPage != null) {
            pageList.add(associationPage)
        }
        return pageList
    }

    private fun buildMessage(messageInfo: MessageInfo) {
        val notifyPost = messageInfo.notifyPost
        if (messageInfo.atomId != null) {
            try {
                val atomInfo =
                    client.get(UserMarketAtomResource::class).getProcessInfo(messageInfo.atomId!!)
                        .data
                if (notifyPost != null) {
                    notifyPost.message = objectMapper.writeValueAsString(atomInfo)
                }
            } catch (e: Exception) {
                logger.error("AmdWebSocketMessage:getAtomById error. message:${e.message}")
            }
        }
    }
}