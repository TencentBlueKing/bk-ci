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
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.v2.ContentAttr
import com.tencent.devops.gitci.pojo.v2.UserMessage
import com.tencent.devops.gitci.pojo.v2.UserMessageRecord
import com.tencent.devops.gitci.pojo.v2.UserMessageType
import com.tencent.devops.gitci.v2.dao.GitUserMessageDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class GitUserMessageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitUserMessageDao: GitUserMessageDao,
    private val gitCIV2RequestService: GitCIV2RequestService
) {
    companion object {
        private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    fun getMessages(
        userId: String,
        messageType: UserMessageType?,
        haveRead: Boolean?,
        page: Int,
        pageSize: Int
    ): Page<UserMessageRecord> {
        // 后续有不同类型再考虑分开逻辑，目前全部按照request处理
        val messageCount = gitUserMessageDao.getMessageCount(dslContext, userId, messageType, haveRead)
        if (messageCount == 0) {
            return Page(
                page = page,
                pageSize = pageSize,
                count = 0,
                records = listOf()
            )
        }
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val messageRecords = gitUserMessageDao.getMessages(
            dslContext = dslContext,
            userId = userId,
            messageType = messageType,
            haveRead = haveRead,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        )!!
        val requestIds = messageRecords.map { it.messageId.toInt() }.toSet()
        val requestMap = gitCIV2RequestService.getRequestMap(userId, requestIds)
        val resultMap = mutableMapOf<String, MutableList<UserMessage>>()
        messageRecords.forEach { message ->
            if (requestMap[message.messageId] == null) {
                return@forEach
            }
            val time = message.createTime.format(timeFormat)
            val content = requestMap[message.messageId]!!
            val failedNum = content.buildRecords.map { it.reason != TriggerReason.TRIGGER_SUCCESS.name }.size
            val userMassage = UserMessage(
                id = message.id,
                userId = message.userId,
                messageType = UserMessageType.valueOf(message.messageType),
                messageTitle = message.messageTitle,
                messageId = message.messageId,
                haveRead = message.haveRead,
                createTime = message.createTime.timestamp(),
                updateTime = message.updateTime.timestamp(),
                content = content,
                contentAttr = ContentAttr(
                    total = content.buildRecords.size,
                    failedNum = failedNum
                )
            )
            if (resultMap.containsKey(time)) {
                resultMap[time]!!.add(userMassage)
            } else {
                resultMap[time] = mutableListOf(userMassage)
            }
        }
        return Page(
            page = page,
            pageSize = pageSize,
            count = messageCount.toLong(),
            records = resultMap.map { UserMessageRecord(time = it.key, records = it.value) }
        )
    }

    fun readMessage(
        userId: String,
        messageId: Int?,
        isAll: Boolean = false
    ): Boolean {
        return if (isAll) {
            gitUserMessageDao.readAllMessage(dslContext, userId) >= 0
        } else {
            gitUserMessageDao.readMessage(dslContext, messageId!!) >= 0
        }
    }

    fun getNoReadMessageCount(
        userId: String
    ): Int {
        return gitUserMessageDao.getNoReadCount(dslContext, userId)
    }
}
