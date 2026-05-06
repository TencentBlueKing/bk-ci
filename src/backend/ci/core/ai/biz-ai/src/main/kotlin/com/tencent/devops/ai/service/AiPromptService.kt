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

import com.tencent.devops.ai.constant.AiInteractionType
import com.tencent.devops.ai.constant.AiMessageCode
import com.tencent.devops.ai.dao.AiPromptDao
import com.tencent.devops.ai.pojo.AiPromptCreate
import com.tencent.devops.ai.pojo.AiPromptInfo
import com.tencent.devops.ai.pojo.AiPromptUpdate
import com.tencent.devops.ai.pojo.SlashPromptVO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.ai.tables.records.TAiPromptRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneOffset

/**
 * 用户自定义提示词管理服务，提供提示词的 CRUD 操作。
 */
@Service
class AiPromptService @Autowired constructor(
    private val dslContext: DSLContext,
    private val aiPromptDao: AiPromptDao,
    private val welcomeGuideService: WelcomeGuideService
) {

    fun createPrompt(
        userId: String,
        request: AiPromptCreate
    ): AiPromptInfo {
        val id = UUIDUtil.generate()
        logger.info(
            "[Prompt] Creating: id={}, userId={}, title={}",
            id, userId, request.title
        )
        val interactionType = AiInteractionType.normalizeUserPromptType(request.interactionType)
        aiPromptDao.create(
            dslContext, id, userId,
            request.title, request.content, interactionType
        )
        val record = aiPromptDao.getById(dslContext, id)
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.CREATE_PROMPT_FAILED,
                defaultMessage = "Failed to create prompt"
            )
        return toPromptInfo(record)
    }

    fun listPrompts(userId: String): List<AiPromptInfo> {
        val result = aiPromptDao.listByUserId(
            dslContext, userId
        ).map { toPromptInfo(it) }
        logger.info(
            "[Prompt] List: userId={}, count={}",
            userId, result.size
        )
        return result
    }

    /**
     * 合并系统预置（欢迎引导 actions）与个人提示词，顺序：系统按卡片与 action 顺序，其后为个人提示词。
     */
    fun listAllPrompts(userId: String, projectId: String?): List<SlashPromptVO> {
        val guide = welcomeGuideService.getWelcomeGuide(userId, projectId)
        val systemItems = guide.cards.flatMap { card ->
            card.actions.map { action ->
                SlashPromptVO(
                    id = action.id,
                    label = action.label,
                    prompt = action.prompt,
                    interactionType = action.interactionType,
                    source = SlashPromptVO.SOURCE_SYSTEM,
                    icon = card.icon,
                    categoryId = card.id,
                    categoryLabel = card.label
                )
            }
        }
        val userItems = listPrompts(userId).map { p ->
            SlashPromptVO(
                id = p.id,
                label = p.title,
                prompt = p.content,
                interactionType = p.interactionType,
                source = SlashPromptVO.SOURCE_USER,
                icon = null,
                categoryId = null,
                categoryLabel = null
            )
        }
        val merged = userItems + systemItems
        return merged
    }

    fun updatePrompt(
        userId: String,
        promptId: String,
        request: AiPromptUpdate
    ): Boolean {
        logger.info(
            "[Prompt] Updating: userId={}, promptId={}, " +
                    "newTitle={}",
            userId, promptId, request.title
        )
        checkOwnership(userId, promptId)
        val existing = aiPromptDao.getById(dslContext, promptId)!!
        val interactionType = request.interactionType?.let {
            AiInteractionType.normalizeUserPromptType(it)
        } ?: AiInteractionType.normalizeUserPromptType(existing.interactionType)
        return aiPromptDao.update(
            dslContext, promptId,
            request.title, request.content, interactionType
        ) > 0
    }

    fun deletePrompt(userId: String, promptId: String): Boolean {
        logger.info(
            "[Prompt] Deleting: userId={}, promptId={}",
            userId, promptId
        )
        checkOwnership(userId, promptId)
        return aiPromptDao.delete(dslContext, promptId) > 0
    }

    private fun checkOwnership(
        userId: String,
        promptId: String
    ) {
        val record = aiPromptDao.getById(dslContext, promptId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.PROMPT_NOT_FOUND,
                defaultMessage = "Prompt not found",
                params = arrayOf(promptId)
            )
        if (record.userId != userId) {
            logger.warn(
                "[Prompt] 权限不足: userId={} " +
                        "尝试操作属于 {} 的提示词",
                userId, record.userId
            )
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = AiMessageCode.PROMPT_NO_PERMISSION,
                defaultMessage = "No permission",
                params = arrayOf(promptId)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiPromptService::class.java
        )
    }

    private fun toPromptInfo(record: TAiPromptRecord): AiPromptInfo {
        return AiPromptInfo(
            id = record.id,
            userId = record.userId,
            title = record.title,
            content = record.content,
            interactionType = AiInteractionType.normalizeUserPromptType(record.interactionType),
            createdTime = record.createdTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
            updatedTime = record.updatedTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
        )
    }
}
