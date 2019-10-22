package com.tencent.devops.store.service.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.message.AmdMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.PageUtils
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.store.service.atom.MarketAtomService
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class AmdWebsocketPush(
    val atomId: String,
    val marketAtomService: MarketAtomService,
    override val userId: String,
    override val pathClass: IPath,
    override val pushType: WebSocketType,
    override val redisOperation: RedisOperation,
    override val objectMapper: ObjectMapper,
    override var page: String?,
    override var notifyPost: NotifyPost
) : WebsocketPush(userId, pathClass, pushType, redisOperation, objectMapper, page, notifyPost) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun findSession(page: String): List<String>? {
        val pageList = createAssociationPage(page!!)

        var sessionList = mutableListOf<String>()
        pageList.forEach {
            val redisSession = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, it)
            if(redisSession != null){
                sessionList.addAll(redisSession)
            }
        }
        return sessionList
    }

    override fun buildMqMessage(): SendMessage {
        val sessionList = findSession(page!!)
        return AmdMessage(
                atomId = atomId,
                notifyPost = notifyPost,
                userId = userId,
                page = page,
                sessionList = sessionList
        )
    }

    override fun buildNotifyMessage(messageInfo: SendMessage) {
        val notifyPost = messageInfo.notifyPost
        if (atomId != null) {
            try {
                val atomInfo = marketAtomService.getProcessInfo(atomId)
                if (notifyPost != null) {
                    notifyPost.message = objectMapper.writeValueAsString(atomInfo)
                }
            } catch (e: Exception) {
                logger.error("AmdWebSocketMessage:getAtomById error. message:${e.message}")
            }
        }
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
}