package com.tencent.devops.store.service.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebsocketService @Autowired constructor(
    val redisOperation: RedisOperation,
    val objectMapper: ObjectMapper,
    val dslContext: DSLContext,
    val atomBaseDao: AtomDao,
    val atomMemberDao: StoreMemberDao,
    val webSocketDispatcher: WebSocketDispatcher
) {

    fun sendWebsocketMessage(
        userId: String,
        atomId: String
    ) {
        val storeWebsocketPush = buildStoreMessage(atomId, userId)
        webSocketDispatcher.dispatch(storeWebsocketPush)
    }

    fun sendWebsocketMessageByAtomCodeAndAtomId(atomCode: String, atomId: String) {
        logger.info("[sendWebsocketMessageByAtomCodeAndAtomId]-atomCode:$atomCode，atomId：$atomId")
        val memberList = atomMemberDao.list(dslContext, atomCode, null, StoreTypeEnum.ATOM.type.toByte())
        if (memberList != null) {
            memberList.forEach {
                sendWebsocketMessage(it.username, atomId)
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

    fun buildStoreMessage(atomId: String, userId: String): StoreWebsocketPush {
        val page = StoryPageBuild().buildPage(
                buildPageInfo = BuildPageInfo(
                        buildId = null,
                        pipelineId = null,
                        projectId = null,
                        atomId = atomId
                ))
        logger.info("store websocket: page[$page], atomId:[$atomId]")
        return StoreWebsocketPush(
                atomId = atomId,
                userId = userId,
                redisOperation = redisOperation,
                page = page,
                pushType = WebSocketType.STORE,
                objectMapper = objectMapper,
                notifyPost = NotifyPost(
                        module = "store",
                        level = NotityLevel.LOW_LEVEL.getLevel(),
                        message = "",
                        dealUrl = null,
                        code = 200,
                        webSocketType = WebSocketType.changWebType(WebSocketType.STORE),
                        page = page
                )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}