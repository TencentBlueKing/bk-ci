/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.store.service.websocket

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
class StoreWebsocketService @Autowired constructor(
    val redisOperation: RedisOperation,
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
        atomMemberDao.list(dslContext, atomCode, null, StoreTypeEnum.ATOM.type.toByte())?.forEach {
            sendWebsocketMessage(it.username, atomId)
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
                atomId = atomId,
                executeCount = null
            )
        )
        logger.info("store websocket: page[$page], atomId:[$atomId]")
        return StoreWebsocketPush(
            atomId = atomId,
            userId = userId,
            redisOperation = redisOperation,
            page = page,
            pushType = WebSocketType.AMD,
            notifyPost = NotifyPost(
                module = "store",
                level = NotityLevel.LOW_LEVEL.getLevel(),
                message = "",
                dealUrl = null,
                code = 200,
                webSocketType = WebSocketType.changWebType(WebSocketType.AMD),
                page = page
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreWebsocketService::class.java)
    }
}
