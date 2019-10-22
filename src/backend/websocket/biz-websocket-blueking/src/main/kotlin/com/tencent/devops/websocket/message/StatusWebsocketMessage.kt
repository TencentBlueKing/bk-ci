package com.tencent.devops.websocket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.message.MqMessage
import com.tencent.devops.common.websocket.dispatch.message.PipelineMqMessage
import com.tencent.devops.common.websocket.pojo.MessageInfo
import com.tencent.devops.common.websocket.utils.PageUtils
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class StatusWebsocketMessage @Autowired constructor(
    val messagingTemplate: SimpMessagingTemplate,
    val objectMapper: ObjectMapper,
    val client: Client,
    val redisOperation: RedisOperation
) : ISendMessage {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun sendWebsocketMessage(messageInfo: MessageInfo) {
        logger.info("PipelineStatusChangeListener, changeType:STATUS, pipelineId: ${messageInfo.pipelineId}")
        buildMessage(messageInfo)

        val defaultPage = messageInfo.page
        val pageList = PageUtils.createAllTagPage(defaultPage)
        pageList.forEach {
            val sessionList = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, it)
            if (sessionList != null) {
                sessionList.forEach { session ->
                    if (session == null) {
                        return@forEach
                    }
                    if (!messageInfo.notifyPost!!.message.isNullOrEmpty()) {
                        messagingTemplate!!.convertAndSend(
                            "/topic/bk/notify/$session",
                            objectMapper.writeValueAsString(messageInfo.notifyPost)
                        )
                        logger.info("StatusWebSocketMessage-send topic to: /topic/bk/notify/$session, page:$it")
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

        val currentTimestamp = System.currentTimeMillis()

//        val status = client.get(ServicePipelineResource::class)
//            .getAllstatus(messageInfo.userId, messageInfo.projectId!!, messageInfo.pipelineId!!).data
//        if (status != null) {
//            val result = status.map {
//                it.pipelineId to PipelineStatus(
//                    it.taskCount,
//                    it.buildCount,
//                    it.lock,
//                    it.canManualStartup,
//                    it.latestBuildStartTime,
//                    it.latestBuildEndTime,
//                    it.latestBuildStatus,
//                    it.latestBuildNum,
//                    it.latestBuildTaskName,
//                    it.latestBuildEstimatedExecutionSeconds,
//                    it.latestBuildId,
//                    currentTimestamp,
//                    it.runningBuildCount,
//                    it.hasCollect
//                )
//            }.toMap()

            val result = null
            if (notifyPost != null) {
                notifyPost.message = objectMapper.writeValueAsString(result)
            }
    }
}
