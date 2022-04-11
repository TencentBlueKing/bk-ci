package com.tencent.devops.stream.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.stream.ws.StreamNotifyWebsocketPush
import com.tencent.devops.stream.ws.StreamPipelineWebsocketPush
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamWebsocketService @Autowired constructor(
    val webSocketDispatcher: WebSocketDispatcher,
    val redisOperation: RedisOperation
) {
    fun pushNotifyWebsocket(userId: String, gitProjectId: String?) {
        val projectCode = if (gitProjectId == null) {
            null
        } else {
            "git_$gitProjectId"
        }
        try {
            webSocketDispatcher.dispatch(
                StreamNotifyWebsocketPush(
                    buildId = null,
                    projectId = projectCode,
                    userId = userId,
                    pushType = WebSocketType.NOTIFY,
                    redisOperation = redisOperation,
                    page = "",
                    notifyPost = NotifyPost(
                        module = "stream",
                        level = 0,
                        dealUrl = null,
                        code = 200,
                        message = "",
                        webSocketType = WebSocketType.changWebType(WebSocketType.NOTIFY),
                        page = ""
                    )
                )
            )
        } catch (e: Exception) {
            logger.warn("Stream V2WebsocketService pushNotifyWebsocket fail $userId $gitProjectId $e")
        }
    }

    fun pushPipelineWebSocket(projectId: String, pipelineId: String, userId: String) {
        val projectCode = "git_$projectId"
        try {
            webSocketDispatcher.dispatch(
                StreamPipelineWebsocketPush(
                    projectId = projectCode,
                    pipelineId = pipelineId,
                    userId = userId,
                    pushType = WebSocketType.STATUS,
                    redisOperation = redisOperation,
                    page = "",
                    notifyPost = NotifyPost(
                        module = "stream",
                        level = 0,
                        code = 200,
                        page = "",
                        webSocketType = WebSocketType.changWebType(WebSocketType.STATUS),
                        message = "",
                        dealUrl = null
                    )
                )
            )
        } catch (e: Exception) {
            logger.warn("Stream V2WebsocketService pushPipelineWebSocket fail $userId $projectCode $e")
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(StreamWebsocketService::class.java)
    }
}
