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

import com.tencent.devops.ai.constant.AiMessageCode
import com.tencent.devops.ai.dao.AiMessageDao
import com.tencent.devops.ai.dao.AiSessionDao
import com.tencent.devops.ai.pojo.AiMessageInfo
import com.tencent.devops.ai.pojo.AiSessionCreate
import com.tencent.devops.ai.pojo.AiSessionInfo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.ai.tables.records.TAiMessageRecord
import com.tencent.devops.model.ai.tables.records.TAiSessionRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneOffset

/**
 * AI 会话管理服务。
 *
 * 提供会话的创建、查询、更新、删除，以及会话消息查询。
 * 每个会话关联一个用户和可选的项目。
 */
@Service
class AiSessionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val aiSessionDao: AiSessionDao,
    private val aiMessageDao: AiMessageDao
) {

    fun createSession(
        userId: String,
        request: AiSessionCreate
    ): AiSessionInfo {
        val id = UUIDUtil.generate()
        val title = request.title ?: DEFAULT_TITLE
        logger.info(
            "[Session] Creating: id={}, userId={}, " +
                "projectId={}, title={}",
            id, userId, request.projectId, title
        )
        aiSessionDao.create(
            dslContext, id, userId, request.projectId, title
        )
        val record = aiSessionDao.getById(dslContext, id)
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.CREATE_SESSION_FAILED,
                defaultMessage = "Failed to create session"
            )
        return toSessionInfo(record)
    }

    /**
     * 确保会话存在；若不存在则自动创建。
     *
     * 由 AG-UI 协议驱动调用，保证对话请求始终有关联会话。
     * 新建会话时，若提供了 [firstUserMessage]，则截取前 [MAX_TITLE_LENGTH]
     * 个字符作为会话标题，否则使用默认标题。
     */
    fun ensureSession(
        sessionId: String,
        userId: String,
        projectId: String? = null,
        firstUserMessage: String? = null
    ) {
        val title = deriveTitle(firstUserMessage)
        val existing = aiSessionDao.getById(dslContext, sessionId)
        if (existing != null) {
            if (title != null && existing.title == DEFAULT_TITLE) {
                aiSessionDao.updateTitle(dslContext, sessionId, title)
                logger.info(
                    "[Session] Updated default title: id={}, newTitle={}",
                    sessionId, title
                )
            }
            return
        }
        logger.info(
            "[Session] Auto-creating session from AG-UI: " +
                "id={}, userId={}, projectId={}, title={}",
            sessionId, userId, projectId, title ?: DEFAULT_TITLE
        )
        aiSessionDao.create(
            dslContext, sessionId, userId,
            projectId, title ?: DEFAULT_TITLE
        )
    }

    /** 从用户首条消息提取标题，无有效内容时返回 null。 */
    private fun deriveTitle(firstUserMessage: String?): String? {
        return firstUserMessage
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(MAX_TITLE_LENGTH)
            ?.ifBlank { null }
    }

    fun listSessions(
        userId: String,
        projectId: String?
    ): List<AiSessionInfo> {
        val result = aiSessionDao.listByUserAndProject(
            dslContext, userId, projectId
        ).map { toSessionInfo(it) }
        logger.info(
            "[Session] List: userId={}, projectId={}, " +
                "count={}",
            userId, projectId, result.size
        )
        return result
    }

    fun getLatestSession(
        userId: String,
        projectId: String?
    ): AiSessionInfo? {
        val result = aiSessionDao.getLatest(
            dslContext, userId, projectId
        )?.let { toSessionInfo(it) }
        logger.info(
            "[Session] GetLatest: userId={}, projectId={}, " +
                "found={}",
            userId, projectId, result != null
        )
        return result
    }

    fun updateTitle(
        userId: String,
        sessionId: String,
        title: String
    ): Boolean {
        logger.info(
            "[Session] UpdateTitle: userId={}, " +
                "sessionId={}, newTitle={}",
            userId, sessionId, title
        )
        val session = aiSessionDao.getById(dslContext, sessionId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.SESSION_NOT_FOUND,
                defaultMessage = "Session not found",
                params = arrayOf(sessionId)
            )
        if (session.userId != userId) {
            logger.warn(
                "[Session] 权限不足: userId={} " +
                    "尝试更新属于 {} 的会话",
                userId, session.userId
            )
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = AiMessageCode.SESSION_NO_PERMISSION,
                defaultMessage = "No permission",
                params = arrayOf(sessionId)
            )
        }
        return aiSessionDao.updateTitle(
            dslContext, sessionId, title
        ) > 0
    }

    fun deleteSession(
        userId: String,
        sessionId: String
    ): Boolean {
        logger.info(
            "[Session] Delete: userId={}, sessionId={}",
            userId, sessionId
        )
        val session = aiSessionDao.getById(dslContext, sessionId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.SESSION_NOT_FOUND,
                defaultMessage = "Session not found",
                params = arrayOf(sessionId)
            )
        if (session.userId != userId) {
            logger.warn(
                "[Session] 权限不足: userId={} " +
                    "尝试删除属于 {} 的会话",
                userId, session.userId
            )
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = AiMessageCode.SESSION_NO_PERMISSION,
                defaultMessage = "No permission",
                params = arrayOf(sessionId)
            )
        }
        val msgCount = aiMessageDao.deleteBySessionId(
            dslContext, sessionId
        )
        logger.info(
            "[Session] 级联删除 {} 条消息, " +
                "会话={}",
            msgCount, sessionId
        )
        return aiSessionDao.delete(dslContext, sessionId) > 0
    }

    fun getMessages(
        userId: String,
        sessionId: String
    ): List<AiMessageInfo> {
        val session = aiSessionDao.getById(dslContext, sessionId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.SESSION_NOT_FOUND,
                defaultMessage = "Session not found",
                params = arrayOf(sessionId)
            )
        if (session.userId != userId) {
            logger.warn(
                "[Session] 权限不足: userId={} " +
                    "尝试访问属于 {} 的会话",
                userId, session.userId
            )
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = AiMessageCode.SESSION_NO_PERMISSION,
                defaultMessage = "No permission",
                params = arrayOf(sessionId)
            )
        }
        val result = aiMessageDao.listBySessionId(
            dslContext, sessionId
        ).map { toMessageInfo(it) }
        logger.info(
            "[Session] GetMessages: sessionId={}, count={}",
            sessionId, result.size
        )
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiSessionService::class.java
        )
        private const val DEFAULT_TITLE = "新对话"
        /** 会话标题最大字符数 */
        private const val MAX_TITLE_LENGTH = 50
    }

    private fun toSessionInfo(record: TAiSessionRecord): AiSessionInfo {
        return AiSessionInfo(
            id = record.id,
            userId = record.userId,
            projectId = record.projectId,
            title = record.title,
            createdTime = record.createdTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
            updatedTime = record.updatedTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
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
