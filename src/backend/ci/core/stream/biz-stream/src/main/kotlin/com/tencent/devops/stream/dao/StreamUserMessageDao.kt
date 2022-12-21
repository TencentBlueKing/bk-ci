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

package com.tencent.devops.stream.dao

import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.model.stream.tables.TGitUserMessage
import com.tencent.devops.model.stream.tables.records.TGitUserMessageRecord
import com.tencent.devops.stream.pojo.message.UserMessageType
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StreamUserMessageDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        messageType: UserMessageType,
        messageTitle: String,
        messageId: String,
        haveRead: Boolean = false
    ) {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                USER_ID,
                MESSAGE_TYPE,
                MESSAGE_TITLE,
                MESSAGE_ID,
                HAVE_READ
            ).values(
                projectId,
                userId,
                messageType.name,
                // 临时截断后续评估如何展示
                CommonUtils.interceptStringInLength(messageTitle, 250),
                messageId,
                haveRead
            ).execute()
        }
    }

    fun getMessages(
        dslContext: DSLContext,
        userId: String?,
        projectId: String,
        messageType: UserMessageType?,
        haveRead: Boolean?,
        messageId: String?,
        offset: Int,
        limit: Int
    ): List<TGitUserMessageRecord>? {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            val conditions = mutableListOf<Condition>()
            if (!userId.isNullOrBlank()) {
                conditions.add(USER_ID.eq(userId))
            }
            if (projectId.isNotBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (messageType != null) {
                conditions.add(MESSAGE_TYPE.eq(messageType.name))
            }
            if (haveRead != null) {
                conditions.add(HAVE_READ.eq(haveRead))
            }
            if (messageId != null) {
                conditions.add(MESSAGE_ID.eq(messageId))
            }
            return dslContext.selectFrom(this).where(conditions).orderBy(ID.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getMessageCount(
        dslContext: DSLContext,
        userId: String?,
        projectId: String,
        messageType: UserMessageType?,
        messageId: String?,
        haveRead: Boolean?
    ): Int {
        return selectMessageCount(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            messageType = messageType,
            messageId = messageId,
            haveRead = haveRead
        )
    }

    fun readMessage(
        dslContext: DSLContext,
        id: Int
    ): Int {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            return dslContext.update(this)
                .set(HAVE_READ, true)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun readAllMessage(
        dslContext: DSLContext,
        userId: String?,
        projectCode: String
    ): Int {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            val dsl = dslContext.update(this)
                .set(HAVE_READ, true)
                .where(PROJECT_ID.eq(projectCode))
            if (!userId.isNullOrBlank()) {
                dsl.and(USER_ID.eq(userId))
            }
            return dsl.execute()
        }
    }

    fun readAllMessage(
        dslContext: DSLContext,
        userId: String
    ): Int {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            return dslContext.update(this)
                .set(HAVE_READ, true)
                .where(USER_ID.eq(userId))
                .execute()
        }
    }

    fun getNoReadCount(
        dslContext: DSLContext,
        userId: String?,
        projectId: String
    ): Int {
        val select = if (userId != null) {
            selectMessageCount(
                dslContext = dslContext,
                userId = userId,
                messageType = null,
                messageId = null,
                haveRead = false
            )
        } else {
            selectMessageCount(
                dslContext = dslContext,
                projectId = projectId,
                messageType = null,
                messageId = null,
                haveRead = false
            )
        }
        return select
    }

    fun getMessageExist(
        dslContext: DSLContext,
        projectId: String,
        userId: String?,
        messageId: String
    ): TGitUserMessageRecord? {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            val dsl = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (userId != null) {
                dsl.and(USER_ID.eq(userId))
            }
            return dsl.and(MESSAGE_ID.eq(messageId))
                .fetchAny()
        }
    }

    fun updateMessageType(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        messageId: String,
        messageType: UserMessageType
    ) {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            dslContext.update(this)
                .set(MESSAGE_TYPE, messageType.name)
                .where(PROJECT_ID.eq(projectId))
                .and(USER_ID.eq(userId))
                .and(MESSAGE_ID.eq(messageId))
                .execute()
        }
    }

    fun deleteByMessageId(
        dslContext: DSLContext,
        projectId: String,
        messageId: String,
        messageType: UserMessageType
    ): Int {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(MESSAGE_ID.eq(messageId))
                .and(MESSAGE_TYPE.eq(messageType.name))
                .execute()
        }
    }

    private fun selectMessageCount(
        dslContext: DSLContext,
        projectId: String,
        messageType: UserMessageType?,
        messageId: String?,
        haveRead: Boolean?
    ): Int {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            val dsl = dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
            if (messageType != null) {
                dsl.and(MESSAGE_TYPE.eq(messageType.name))
            }
            if (haveRead != null) {
                dsl.and(HAVE_READ.eq(haveRead))
            }
            if (messageId != null) {
                dsl.and(MESSAGE_ID.eq(messageId))
            }
            return dsl.fetchOne(0, Int::class.java)!!
        }
    }

    private fun selectMessageCount(
        dslContext: DSLContext,
        userId: String?,
        projectId: String? = null,
        messageType: UserMessageType?,
        messageId: String?,
        haveRead: Boolean?
    ): Int {
        with(TGitUserMessage.T_GIT_USER_MESSAGE) {
            val conditions = mutableListOf<Condition>()
            if (!userId.isNullOrBlank()) {
                conditions.add(USER_ID.eq(userId))
            }
            if (!projectId.isNullOrBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (messageType != null) {
                conditions.add(MESSAGE_TYPE.eq(messageType.name))
            }
            if (haveRead != null) {
                conditions.add(HAVE_READ.eq(haveRead))
            }
            if (messageId != null) {
                conditions.add(MESSAGE_ID.eq(messageId))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }
}
