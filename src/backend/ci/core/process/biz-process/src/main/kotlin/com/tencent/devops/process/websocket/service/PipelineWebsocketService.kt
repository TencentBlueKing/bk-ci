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

package com.tencent.devops.process.websocket.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.process.websocket.page.DetailPageBuild
import com.tencent.devops.process.websocket.page.HistoryPageBuild
import com.tencent.devops.process.websocket.page.RecordPageBuild
import com.tencent.devops.process.websocket.page.StatusPageBuild
import com.tencent.devops.process.websocket.push.DetailWebsocketPush
import com.tencent.devops.process.websocket.push.HistoryWebsocketPush
import com.tencent.devops.process.websocket.push.RecordWebsocketPush
import com.tencent.devops.process.websocket.push.StatusWebsocketPush
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineWebsocketService @Autowired constructor(
    val redisOperation: RedisOperation,
    val historyPageBuild: HistoryPageBuild,
    val detailPageBuild: DetailPageBuild,
    val statusPageBuild: StatusPageBuild,
    val recordPageBuild: RecordPageBuild
) {
    fun buildDetailMessage(
        buildId: String,
        projectId: String,
        pipelineId: String,
        userId: String
    ): DetailWebsocketPush {
        val page = detailPageBuild.buildPage(
            buildPageInfo = BuildPageInfo(
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = projectId,
                atomId = null,
                executeCount = null
            )
        )
        logger.debug("detail websocket: page[$page], buildId:[$buildId],pipelineId:[$pipelineId],project:[$projectId]")
        return DetailWebsocketPush(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            redisOperation = redisOperation,
            page = page,
            pushType = WebSocketType.DETAIL,
            notifyPost = NotifyPost(
                module = "process",
                level = NotityLevel.LOW_LEVEL.getLevel(),
                message = "",
                dealUrl = null,
                code = 200,
                webSocketType = WebSocketType.changWebType(WebSocketType.DETAIL),
                page = page
            )
        )
    }

    fun buildHistoryMessage(
        buildId: String,
        projectId: String,
        pipelineId: String,
        userId: String
    ): HistoryWebsocketPush {
        val page = historyPageBuild.buildPage(
            buildPageInfo = BuildPageInfo(
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = projectId,
                atomId = null,
                executeCount = null
            )
        )
        logger.debug("history websocket: page[$page], buildId:[$buildId],pipelineId:[$pipelineId],project:[$projectId]")
        return HistoryWebsocketPush(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            redisOperation = redisOperation,
            page = page,
            pushType = WebSocketType.HISTORY,
            notifyPost = NotifyPost(
                module = "process",
                level = NotityLevel.LOW_LEVEL.getLevel(),
                message = "",
                dealUrl = null,
                code = 200,
                webSocketType = WebSocketType.changWebType(WebSocketType.HISTORY),
                page = page
            )
        )
    }

    fun buildStatusMessage(
        buildId: String,
        projectId: String,
        pipelineId: String,
        userId: String
    ): StatusWebsocketPush {
        val page = statusPageBuild.buildPage(
            buildPageInfo = BuildPageInfo(
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = projectId,
                atomId = null,
                executeCount = null
            )
        )
        logger.debug("status websocket: page[$page], buildId:[$buildId],pipelineId:[$pipelineId],project:[$projectId]")
        return StatusWebsocketPush(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            redisOperation = redisOperation,
            page = page,
            pushType = WebSocketType.STATUS,
            notifyPost = NotifyPost(
                module = "process",
                level = NotityLevel.LOW_LEVEL.getLevel(),
                message = "",
                dealUrl = null,
                code = 200,
                webSocketType = WebSocketType.changWebType(WebSocketType.STATUS),
                page = page
            )
        )
    }

    fun buildRecordMessage(
        buildId: String,
        projectId: String,
        pipelineId: String,
        userId: String,
        executeCount: Int?
    ): RecordWebsocketPush {
        val page = recordPageBuild.buildPage(
            buildPageInfo = BuildPageInfo(
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = projectId,
                atomId = null,
                executeCount = executeCount
            )
        )
        logger.debug("record websocket: page[$page], buildId:[$buildId],pipelineId:[$pipelineId],project:[$projectId]")
        return RecordWebsocketPush(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            executeCount = executeCount,
            redisOperation = redisOperation,
            page = page,
            pushType = WebSocketType.RECORD,
            notifyPost = NotifyPost(
                module = "process",
                level = NotityLevel.LOW_LEVEL.getLevel(),
                message = "",
                dealUrl = null,
                code = 200,
                webSocketType = WebSocketType.changWebType(WebSocketType.RECORD),
                page = page
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineWebsocketService::class.java)
    }
}
