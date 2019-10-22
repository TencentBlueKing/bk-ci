package com.tencent.devops.common.websocket.dispatch.push

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory

abstract class WebsocketPush(
    open val userId: String,
    open val pathClass: IPath,
    open val pushType: WebSocketType,
    open val redisOperation: RedisOperation,
    open val objectMapper: ObjectMapper,
    open var page: String?,
    open var notifyPost: NotifyPost
) {
    companion object {
        val logger = LoggerFactory.getLogger(this:: class.java)
    }

    open fun findSession(page: String): List<String>?
    {
        return RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, page)
    }

    abstract fun buildMqMessage(): SendMessage?

    abstract fun buildNotifyMessage(message: SendMessage)
}