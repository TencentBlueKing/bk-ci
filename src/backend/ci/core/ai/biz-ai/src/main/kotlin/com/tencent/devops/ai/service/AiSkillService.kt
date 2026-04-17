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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.ai.constant.AiMessageCode
import com.tencent.devops.ai.dao.AiSkillDao
import com.tencent.devops.ai.pojo.AiSkillCreate
import com.tencent.devops.ai.pojo.AiSkillInfo
import com.tencent.devops.ai.pojo.AiSkillUpdate
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.ai.tables.records.TAiSkillRecord
import io.agentscope.core.skill.AgentSkill
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneOffset

/**
 * 智能体技能管理服务。
 *
 * 支持系统级和用户级技能配置，同名技能时用户配置覆盖系统配置。
 */
@Service
class AiSkillService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dao: AiSkillDao
) {

    fun createSkill(
        userId: String,
        request: AiSkillCreate
    ): AiSkillInfo {
        val id = UUIDUtil.generate()
        logger.info(
            "[Skill] Creating: id={}, userId={}, name={}",
            id, userId, request.skillName
        )
        dao.create(
            dslContext = dslContext,
            id = id,
            scope = SCOPE_USER,
            userId = userId,
            skillName = request.skillName,
            description = request.description,
            skillContent = request.skillContent,
            resources = request.resources,
            bindAgent = request.bindAgent,
            enabled = request.enabled
        )
        val record = dao.getById(dslContext, id)
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.CREATE_SKILL_FAILED,
                defaultMessage = "Failed to create skill"
            )
        return toInfo(record)
    }

    fun listForUser(userId: String): List<AiSkillInfo> {
        val merged = getMergedSkills(userId)
        logger.info(
            "[Skill] List for user: userId={}, total={}",
            userId, merged.size
        )
        return merged
    }

    fun updateSkill(
        userId: String,
        skillId: String,
        request: AiSkillUpdate
    ): Boolean {
        logger.info(
            "[Skill] Updating: userId={}, skillId={}",
            userId, skillId
        )
        checkOwnership(userId, skillId)
        return dao.update(
            dslContext = dslContext,
            id = skillId,
            skillName = request.skillName,
            description = request.description,
            skillContent = request.skillContent,
            resources = request.resources,
            bindAgent = request.bindAgent,
            enabled = request.enabled
        ) > 0
    }

    fun deleteSkill(userId: String, skillId: String): Boolean {
        logger.info(
            "[Skill] Deleting: userId={}, skillId={}",
            userId, skillId
        )
        checkOwnership(userId, skillId)
        return dao.delete(dslContext, skillId) > 0
    }

    /**
     * 合并系统级和用户级技能，同名时用户配置优先。
     *
     * @param bindAgent 按目标智能体过滤；null 表示不过滤
     */
    fun getMergedSkills(
        userId: String?,
        bindAgent: String? = null
    ): List<AiSkillInfo> {
        val systemSkills = dao.listEnabled(
            dslContext, SCOPE_SYSTEM, bindAgent
        ).map { toInfo(it) }

        val userSkills = if (userId != null) {
            dao.listEnabledByUser(
                dslContext, userId, bindAgent
            ).map { toInfo(it) }
        } else {
            emptyList()
        }

        val userNames = userSkills.map { it.skillName }.toSet()

        return systemSkills.filter { it.skillName !in userNames } + userSkills
    }

    /**
     * 将数据库记录转换为 agentscope-java 框架的
     * [AgentSkill] 对象。
     */
    fun toAgentSkill(info: AiSkillInfo): AgentSkill {
        val resources = parseResources(info.resources)
        return AgentSkill(
            info.skillName,
            info.description,
            info.skillContent,
            resources,
            info.scope.lowercase()
        )
    }

    private fun parseResources(
        resourcesJson: String?
    ): Map<String, String> {
        if (resourcesJson.isNullOrBlank()) return emptyMap()
        return try {
            objectMapper.readValue(
                resourcesJson,
                object : TypeReference<Map<String, String>>() {}
            )
        } catch (e: Exception) {
            logger.warn(
                "[Skill] Failed to parse resources JSON: {}",
                e.message
            )
            emptyMap()
        }
    }

    private fun checkOwnership(userId: String, skillId: String) {
        val record = dao.getById(dslContext, skillId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.SKILL_NOT_FOUND,
                defaultMessage = "Skill not found",
                params = arrayOf(skillId)
            )
        if (record.scope != SCOPE_USER || record.userId != userId) {
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = AiMessageCode.SKILL_NO_PERMISSION,
                defaultMessage = "No permission",
                params = arrayOf(skillId)
            )
        }
    }

    private fun toInfo(record: TAiSkillRecord): AiSkillInfo {
        return AiSkillInfo(
            id = record.id,
            scope = record.scope,
            userId = record.userId,
            skillName = record.skillName,
            description = record.description,
            skillContent = record.skillContent,
            resources = record.resources,
            bindAgent = record.bindAgent,
            enabled = record.enabled,
            createdTime = record.createdTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
            updatedTime = record.updatedTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiSkillService::class.java
        )
        private val objectMapper = jacksonObjectMapper()
        const val SCOPE_SYSTEM = "SYSTEM" // 系统级配置
        const val SCOPE_USER = "USER" // 用户级配置
    }
}
