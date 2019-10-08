package com.tencent.devops.common.websocket.dispatch.push

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.WebsocketPushDispatcher
import com.tencent.devops.common.websocket.dispatch.message.MqMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory

abstract class IWebsocketPush(
    open val userId: String,
    open val pathClass: IPath,
    open val pushType: WebSocketType,
    open val redisOperation: RedisOperation,
    open val objectMapper: ObjectMapper,
    open var page: String?,
    open var notifyPost: NotifyPost
) {
    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    open fun isPushBySession(): Boolean
    {
        val sessionIdList = RedisUtlis.getSessionIdByUserId(redisOperation, userId)
        WebsocketPushDispatcher.logger.info("WebsocketPushDispatcher: userId:$userId, sessionList:$sessionIdList")
        if (sessionIdList != null) {
            if (sessionIdList.contains(",")) {
                sessionIdList.split(",").forEach {
                    if (RedisUtlis.isSessionLoadPage(redisOperation, it)) {
                        logger.info("[IWebsocketPush]:session[$it],user:[$userId],命中page:[${RedisUtlis.getPageFromSessionPageBySession(redisOperation, it)}]")
                        return true
                    }
                }
            } else {
                logger.info("[IWebsocketPush]:session[$sessionIdList],user:[$userId],命中page情况：${RedisUtlis.isSessionLoadPage(redisOperation, sessionIdList)}}]")
                return RedisUtlis.isSessionLoadPage(redisOperation, sessionIdList)
            }
        }
        logger.info("[IWebsocketPush]:userId:$userId,session[${sessionIdList}未命中任何page]")
        return false
    }

    abstract fun isPushByPage(): Boolean

    abstract fun mqMessage(): MqMessage

    abstract fun buildSendMessage(): SendMessage

    abstract fun buildMessage(messageInfo: IWebsocketPush)
}
