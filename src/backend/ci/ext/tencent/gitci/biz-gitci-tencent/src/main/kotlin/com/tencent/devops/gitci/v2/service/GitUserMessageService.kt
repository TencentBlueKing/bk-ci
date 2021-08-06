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

package com.tencent.devops.gitci.v2.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.v2.message.ContentAttr
import com.tencent.devops.gitci.pojo.v2.message.UserMessage
import com.tencent.devops.gitci.pojo.v2.message.UserMessageRecord
import com.tencent.devops.gitci.pojo.v2.message.UserMessageType
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.gitci.v2.dao.GitUserMessageDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class GitUserMessageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitUserMessageDao: GitUserMessageDao,
    private val gitCIV2RequestService: GitCIV2RequestService,
    private val websocketService: GitCIV2WebsocketService
) {
    companion object {
        private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val logger = LoggerFactory.getLogger(GitUserMessageService::class.java)
    }

    fun getMessages(
        projectId: String?,
        userId: String,
        messageType: UserMessageType?,
        haveRead: Boolean?,
        page: Int,
        pageSize: Int
    ): Page<UserMessageRecord> {
        val startEpoch = System.currentTimeMillis()
        val gitProjectId = if (projectId == null) {
            null
        } else {
            GitCommonUtils.getGitProjectId(projectId)
        }
        // 后续有不同类型再考虑分开逻辑，目前全部按照request处理
        val messageCount = gitUserMessageDao.getMessageCount(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            messageType = messageType,
            haveRead = haveRead
        )
        if (messageCount == 0) {
            return Page(
                page = page,
                pageSize = pageSize,
                count = 0,
                records = listOf()
            )
        }

        logger.info("getMessageTest took ${System.currentTimeMillis() - startEpoch}ms to get messageCount")

        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val messageRecords = gitUserMessageDao.getMessages(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            messageType = messageType,
            haveRead = haveRead,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        )!!

        logger.info("getMessageTest took ${System.currentTimeMillis() - startEpoch}ms to get messageRecords")

        val requestIds = messageRecords.map { it.messageId.toInt() }.toSet()
        val eventMap = gitCIV2RequestService.getRequestMap(userId, gitProjectId, requestIds)

        logger.info("getMessageTest took ${System.currentTimeMillis() - startEpoch}ms to get eventMap")

        val resultMap = mutableMapOf<String, MutableList<UserMessage>>()
        messageRecords.forEach { message ->
            val eventId = message.messageId.toLong()
            if (eventMap[eventId] == null) {
                return@forEach
            }
            val time = message.createTime.format(timeFormat)
            val content = eventMap[eventId]!!
            val failedNum = content.filter { it.triggerReasonName != TriggerReason.TRIGGER_SUCCESS.name }.size
            val userMassage = UserMessage(
                id = message.id,
                userId = message.userId,
                messageType = UserMessageType.valueOf(message.messageType),
                messageTitle = message.messageTitle,
                messageId = message.messageId,
                haveRead = message.haveRead,
                createTime = message.createTime.timestampmilli(),
                updateTime = message.updateTime.timestampmilli(),
                content = content,
                contentAttr = ContentAttr(
                    total = content.size,
                    failedNum = failedNum
                )
            )
            if (resultMap[time].isNullOrEmpty()) {
                resultMap[time] = mutableListOf(userMassage)
            } else {
                resultMap[time]!!.add(userMassage)
            }
        }

        logger.info("getMessageTest took ${System.currentTimeMillis() - startEpoch}ms to get result")

        return Page(
            page = page,
            pageSize = pageSize,
            count = messageCount.toLong(),
            records = resultMap.map { UserMessageRecord(time = it.key, records = it.value) }
        )
    }

    fun readMessage(
        userId: String,
        id: Int,
        projectId: String?
    ): Boolean {
        websocketService.pushNotifyWebsocket(userId, projectId)
        return gitUserMessageDao.readMessage(dslContext, id) >= 0
    }

    fun readAllMessage(
        projectId: String?,
        userId: String
    ): Boolean {
        websocketService.pushNotifyWebsocket(userId, projectId)
        return gitUserMessageDao.readAllMessage(dslContext = dslContext, projectId = projectId, userId = userId) >= 0
    }

    fun getNoReadMessageCount(
        projectId: String?,
        userId: String
    ): Int {
        return gitUserMessageDao.getNoReadCount(dslContext = dslContext, projectId = projectId, userId = userId)
    }
}
