package com.tencent.devops.store.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.websocket.AmdWebsocketPush
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreWebsocketService @Autowired constructor(
        val redisOperation: RedisOperation,
        val marketAtomService: MarketAtomService,
        val objectMapper: ObjectMapper,
        val websocketPushDispatcher: WebsocketPushDispatcher,
        val atomMemberDao: StoreMemberDao,
        val dslContext: DSLContext,
        val atomBaseDao: AtomDao
) {
    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }

    fun sendWebsocketMessage(
        userId: String,
        atomId: String,
        message: String? = null
    ) {
        val push = AmdWebsocketPush(
            userId = userId,
            pathClass = StoryPageBuild(),
            pushType = WebSocketType.STORE,
            atomId = atomId,
            page = null,
            redisOperation = redisOperation,
            objectMapper = objectMapper,
            marketAtomService = marketAtomService,
            notifyPost = NotifyPost(
                module = "store",
                level = NotityLevel.LOW_LEVEL.getLevel(),
                dealUrl = null,
                message = "",
                webSocketType = WebSocketType.changWebType(WebSocketType.STORE),
                code = 200
            )
        )
        logger.info("[StoreWebsocketService]-push:$push")
        websocketPushDispatcher.dispatch(push)
    }

    fun sendWebsocketMessageByAtomCodeAndAtomId(atomCode: String, atomId: String) {
        logger.info("[sendWebsocketMessageByAtomCodeAndAtomId]-atomCode:$atomCode，atomId：$atomId")
        val memberList = atomMemberDao.list(dslContext, atomCode, null, StoreTypeEnum.ATOM.type.toByte())
        if (memberList != null) {
            memberList.forEach {
                sendWebsocketMessage(it.username, atomId, "approve")
            }
        }
    }

    fun sendWebsocketMessageByAtomCodeAndUserId(atomCode: String, userId: String) {
        logger.info("[sendWebsocketMessageByAtomCodeAndUserId]-atomCode:$atomCode，userId：$userId")
//        val atomInfo = marketAtomDao.getAtomsByAtomCode(dslContext, atomCode)
        val atomInfo = atomBaseDao.getNewestAtomByCode(dslContext, atomCode)
        if (atomInfo != null) {
            sendWebsocketMessage(userId, atomInfo.id)
        }
    }
}