package com.tencent.devops.misc.service

import MiscNodeWebsocketPush
import NodePath
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MiscNodeWebsocketService @Autowired constructor(
    val objectMapper: ObjectMapper,
    val redisOperation: RedisOperation
) {
    fun buildDetailMessage(
        projectId: String,
        userId: String
    ): MiscNodeWebsocketPush {
        val page = NodePath().buildPage(
                buildPageInfo = BuildPageInfo(
                        buildId = null,
                        pipelineId = null,
                        projectId = projectId,
                        atomId = null
                )
        )
        logger.info("nodeList websocket: page[$page],project:[$projectId]")
        return MiscNodeWebsocketPush(
                projectId = projectId,
                userId = userId,
                redisOperation = redisOperation,
                page = page,
                pushType = WebSocketType.DETAIL,
                objectMapper = objectMapper,
                notifyPost = NotifyPost(
                        module = "environment",
                        level = NotityLevel.LOW_LEVEL.getLevel(),
                        message = emptyMap<String, String>().toString(),
                        dealUrl = null,
                        code = 200,
                        webSocketType = WebSocketType.changWebType(WebSocketType.AMD),
                        page = page
                )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}