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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.ai.constant.AiInteractionType
import com.tencent.devops.ai.dao.HotQuestionDao
import com.tencent.devops.ai.dao.WelcomeGuideDao
import com.tencent.devops.ai.pojo.FormSchemaVO
import com.tencent.devops.ai.pojo.HotQuestionPageVO
import com.tencent.devops.ai.pojo.HotQuestionVO
import com.tencent.devops.ai.pojo.WelcomeActionVO
import com.tencent.devops.ai.pojo.WelcomeCardVO
import com.tencent.devops.ai.pojo.WelcomeGuideVO
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.ai.tables.records.TAiWelcomeGuideRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * AI 助手欢迎引导页服务，提供引导卡片和热门问题的查询。
 */
@Service
class WelcomeGuideService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val welcomeGuideDao: WelcomeGuideDao,
    private val hotQuestionDao: HotQuestionDao
) {

    /**
     * 获取欢迎页引导数据。
     * @param userId 用户ID
     * @param projectId 项目ID，用于判断用户角色过滤 actions；为空则返回全量
     */
    fun getWelcomeGuide(userId: String, projectId: String?): WelcomeGuideVO {
        val userRole = getUserRoleInProject(userId, projectId)
        val allRecords = welcomeGuideDao.listAllEnabled(dslContext)
        val cards = allRecords.filter { it.type == GUIDE_TYPE_CARD }
        val actionsByParent = allRecords
            .filter { it.parentId != null }
            .groupBy { it.parentId }

        val cardVOs = cards.mapNotNull { card ->
            val actions = actionsByParent[card.id]
                ?.map { action -> mapActionRecord(action) }
                ?.filter { action -> matchesRoleFilter(action.roleFilter, userRole) }
                ?: emptyList()

            if (actions.isEmpty() && userRole != null) {
                null
            } else {
                WelcomeCardVO(
                    id = card.id,
                    label = card.label,
                    description = card.description,
                    icon = card.icon,
                    actions = actions
                )
            }
        }

        val hotQuestions = hotQuestionDao.listEnabled(
            dslContext, DEFAULT_HOT_QUESTION_LIMIT, 0
        ).map { HotQuestionVO(id = it.id, question = it.question) }

        logger.info(
            "[WelcomeGuide] userId={}, projectId={}, role={}, cards={}, hotQuestions={}",
            userId, projectId, userRole, cardVOs.size, hotQuestions.size
        )

        return WelcomeGuideVO(
            cards = cardVOs,
            hotQuestions = hotQuestions
        )
    }

    fun getHotQuestions(
        page: Int,
        pageSize: Int
    ): HotQuestionPageVO {
        val total = hotQuestionDao.countEnabled(dslContext)
        val safePage = if (page < 1) 1 else page
        val offset = (safePage - 1) * pageSize
        val records = hotQuestionDao.listEnabled(
            dslContext, pageSize, offset
        )
        val questions = records.map {
            HotQuestionVO(id = it.id, question = it.question)
        }

        return HotQuestionPageVO(
            questions = questions,
            total = total,
            page = safePage,
            pageSize = pageSize,
            hasMore = (offset + pageSize) < total
        )
    }

    /**
     * 判断用户在项目中的角色。
     * @return ADMIN / MEMBER / null(未传 projectId 时，表示不按角色过滤)
     */
    private fun getUserRoleInProject(userId: String, projectId: String?): String? {
        if (projectId.isNullOrBlank()) {
            return null
        }
        return try {
            val result = client.get(ServiceProjectAuthResource::class)
                .checkProjectManagerAndMessage(userId = userId, projectId = projectId)
            if (result.isOk() && result.data == true) ROLE_ADMIN else ROLE_MEMBER
        } catch (e: Exception) {
            logger.debug("[WelcomeGuide] User {} is not manager of project {}", userId, projectId)
            ROLE_MEMBER
        }
    }

    /**
     * 判断 action 的 roleFilter 是否匹配当前用户角色。
     * - roleFilter 为空：所有用户可见
     * - userRole 为空（未传 projectId）：返回全量，不过滤
     * - 否则：roleFilter 与 userRole 匹配时可见
     */
    private fun matchesRoleFilter(roleFilter: String?, userRole: String?): Boolean {
        if (roleFilter.isNullOrBlank()) return true
        if (userRole == null) return true
        return roleFilter == userRole
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            WelcomeGuideService::class.java
        )
        private val objectMapper = jacksonObjectMapper()
        private const val GUIDE_TYPE_CARD = "CARD"
        private const val DEFAULT_HOT_QUESTION_LIMIT = 5
        private const val ROLE_ADMIN = "ADMIN"
        private const val ROLE_MEMBER = "MEMBER"

        private fun parseFormSchema(json: String?): FormSchemaVO? {
            if (json.isNullOrBlank()) {
                return null
            }
            return try {
                objectMapper.readValue<FormSchemaVO>(json)
            } catch (e: Exception) {
                logger.warn(
                    "[WelcomeGuide] FORM_SCHEMA parse failed: {}",
                    e.message
                )
                null
            }
        }

        private fun normalizeActionInteractionType(raw: String?): String {
            val t = raw?.trim()?.takeIf { it.isNotEmpty() }
                ?: AiInteractionType.PROMPT_COMPLETION
            return if (t in AiInteractionType.WELCOME_ACTION_ALLOWED) t else AiInteractionType.PROMPT_COMPLETION
        }

        private fun mapActionRecord(action: TAiWelcomeGuideRecord): WelcomeActionVO {
            val interactionType = normalizeActionInteractionType(action.interactionType)
            val formSchema = if (interactionType == AiInteractionType.FORM_COLLECT) {
                parseFormSchema(action.formSchema)
            } else {
                null
            }
            return WelcomeActionVO(
                id = action.id,
                label = action.label,
                prompt = action.promptContent ?: "",
                interactionType = interactionType,
                formSchema = formSchema,
                roleFilter = action.roleFilter?.trim()?.takeIf { it.isNotEmpty() }
            )
        }
    }
}
