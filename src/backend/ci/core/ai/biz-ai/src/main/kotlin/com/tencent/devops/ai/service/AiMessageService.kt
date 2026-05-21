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

package com.tencent.devops.ai.service

import com.tencent.devops.ai.dao.AiMessageDao
import com.tencent.devops.ai.dao.AiSessionDao
import com.tencent.devops.ai.pojo.AiMessageInfo
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.ai.tables.records.TAiMessageRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneOffset

/** AI 消息业务服务，集中封装消息写入、查询和删除。 */
@Service
class AiMessageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val aiMessageDao: AiMessageDao,
    private val aiSessionDao: AiSessionDao
) {

    fun createMessage(
        sessionId: String,
        role: String,
        content: String?,
        extraData: String? = null,
        updateSessionTime: Boolean = true
    ): AiMessageCreateResult? {
        if (content.isNullOrBlank()) return null

        val messageId = UUIDUtil.generate()
        val nextIndex = aiMessageDao.getMaxIndex(
            dslContext = dslContext,
            sessionId = sessionId
        ) + 1
        aiMessageDao.create(
            dslContext = dslContext,
            id = messageId,
            sessionId = sessionId,
            role = role,
            content = content,
            messageIndex = nextIndex,
            extraData = extraData
        )
        if (updateSessionTime) {
            aiSessionDao.updateTime(
                dslContext = dslContext,
                sessionId = sessionId
            )
        }
        return AiMessageCreateResult(
            id = messageId,
            messageIndex = nextIndex
        )
    }

    fun listMessages(sessionId: String): List<AiMessageInfo> {
        return aiMessageDao.listBySessionId(
            dslContext = dslContext,
            sessionId = sessionId
        ).map { toMessageInfo(record = it) }
    }

    fun deleteBySessionId(sessionId: String): Int {
        return aiMessageDao.deleteBySessionId(
            dslContext = dslContext,
            sessionId = sessionId
        )
    }

    private fun toMessageInfo(record: TAiMessageRecord): AiMessageInfo {
        return AiMessageInfo(
            id = record.id,
            sessionId = record.sessionId,
            role = record.role,
            content = record.content,
            extraData = record.extraData,
            createdTime = record.createdTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
        )
    }
}

data class AiMessageCreateResult(
    val id: String,
    val messageIndex: Int
)
