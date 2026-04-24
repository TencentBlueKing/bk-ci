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
import com.tencent.devops.ai.dao.ExternalAgentConfigDao
import com.tencent.devops.ai.pojo.ExternalAgentCreate
import com.tencent.devops.ai.pojo.ExternalAgentInfo
import com.tencent.devops.ai.pojo.ExternalAgentUpdate
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.ai.tables.records.TAiExternalAgentConfigRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneOffset

/**
 * 外部智能体配置管理服务，支持对接第三方智能体平台（如 Knot）。
 */
@Service
class ExternalAgentService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dao: ExternalAgentConfigDao
) {

    fun create(
        userId: String,
        request: ExternalAgentCreate
    ): ExternalAgentInfo {
        val id = UUIDUtil.generate()
        logger.info(
            "[ExternalAgent] Creating: id={}, userId={}, " +
                "name={}, platform={}",
            id, userId, request.agentName, request.platform
        )
        dao.create(
            dslContext = dslContext,
            id = id,
            userId = userId,
            agentName = request.agentName,
            description = request.description,
            platform = request.platform,
            agentId = request.agentId,
            apiUrl = request.apiUrl,
            headers = request.headers,
            enabled = request.enabled
        )
        val record = dao.getById(dslContext, id)
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.CREATE_EXTERNAL_AGENT_FAILED,
                defaultMessage =
                    "Failed to create external agent config"
            )
        return toInfo(record)
    }

    fun list(userId: String): List<ExternalAgentInfo> {
        return dao.listByUser(dslContext, userId)
            .map { toInfo(it) }
    }

    fun listEnabled(userId: String?): List<ExternalAgentInfo> {
        if (userId.isNullOrBlank()) return emptyList()
        return dao.listEnabledByUser(dslContext, userId)
            .map { toInfo(it) }
    }

    fun update(
        userId: String,
        configId: String,
        request: ExternalAgentUpdate
    ): Boolean {
        logger.info(
            "[ExternalAgent] Updating: userId={}, configId={}",
            userId, configId
        )
        checkOwnership(userId, configId)
        return dao.update(
            dslContext = dslContext,
            id = configId,
            agentName = request.agentName,
            description = request.description,
            platform = request.platform,
            agentId = request.agentId,
            apiUrl = request.apiUrl,
            headers = request.headers,
            enabled = request.enabled
        ) > 0
    }

    fun delete(userId: String, configId: String): Boolean {
        logger.info(
            "[ExternalAgent] Deleting: userId={}, configId={}",
            userId, configId
        )
        checkOwnership(userId, configId)
        return dao.delete(dslContext, configId) > 0
    }

    private fun checkOwnership(userId: String, configId: String) {
        val record = dao.getById(dslContext, configId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.EXTERNAL_AGENT_NOT_FOUND,
                defaultMessage =
                    "External agent config not found",
                params = arrayOf(configId)
            )
        if (record.userId != userId) {
            throw ErrorCodeException(
                statusCode = 403,
                errorCode = AiMessageCode.EXTERNAL_AGENT_NO_PERMISSION,
                defaultMessage = "No permission",
                params = arrayOf(configId)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            ExternalAgentService::class.java
        )
    }

    private fun toInfo(
        record: TAiExternalAgentConfigRecord
    ): ExternalAgentInfo {
        return ExternalAgentInfo(
            id = record.id,
            userId = record.userId,
            agentName = record.agentName,
            description = record.description,
            platform = record.platform,
            agentId = record.agentId,
            apiUrl = record.apiUrl,
            headers = record.headers,
            enabled = record.enabled,
            createdTime = record.createdTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
            updatedTime = record.updatedTime
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
        )
    }
}
