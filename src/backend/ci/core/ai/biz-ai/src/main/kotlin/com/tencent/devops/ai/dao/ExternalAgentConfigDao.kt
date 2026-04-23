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

package com.tencent.devops.ai.dao

import com.tencent.devops.model.ai.tables.TAiExternalAgentConfig
import com.tencent.devops.model.ai.tables.records.TAiExternalAgentConfigRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/** 外部智能体配置 DAO，对应 T_AI_EXTERNAL_AGENT_CONFIG 表。 */
@Repository
class ExternalAgentConfigDao {

    fun create(
        dslContext: DSLContext,
        id: String,
        userId: String,
        agentName: String,
        description: String,
        platform: String,
        agentId: String,
        apiUrl: String,
        headers: String?,
        enabled: Boolean
    ) {
        val now = LocalDateTime.now()
        with(TAiExternalAgentConfig.T_AI_EXTERNAL_AGENT_CONFIG) {
            dslContext.insertInto(
                this,
                ID, USER_ID, AGENT_NAME, DESCRIPTION, PLATFORM,
                AGENT_ID, API_URL, HEADERS, ENABLED,
                CREATED_TIME, UPDATED_TIME
            ).values(
                id, userId, agentName, description, platform,
                agentId, apiUrl, headers,
                enabled, now, now
            ).execute()
        }
    }

    fun getById(
        dslContext: DSLContext,
        id: String
    ): TAiExternalAgentConfigRecord? {
        with(TAiExternalAgentConfig.T_AI_EXTERNAL_AGENT_CONFIG) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun listByUser(
        dslContext: DSLContext,
        userId: String
    ): Result<TAiExternalAgentConfigRecord> {
        with(TAiExternalAgentConfig.T_AI_EXTERNAL_AGENT_CONFIG) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .orderBy(CREATED_TIME.desc())
                .fetch()
        }
    }

    fun listEnabledByUser(
        dslContext: DSLContext,
        userId: String
    ): Result<TAiExternalAgentConfigRecord> {
        with(TAiExternalAgentConfig.T_AI_EXTERNAL_AGENT_CONFIG) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .and(ENABLED.eq(true))
                .orderBy(CREATED_TIME.asc())
                .fetch()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: String,
        agentName: String?,
        description: String?,
        platform: String?,
        agentId: String?,
        apiUrl: String?,
        headers: String?,
        enabled: Boolean?
    ): Int {
        with(TAiExternalAgentConfig.T_AI_EXTERNAL_AGENT_CONFIG) {
            val step = dslContext.update(this)
                .set(UPDATED_TIME, LocalDateTime.now())
            if (agentName != null) step.set(AGENT_NAME, agentName)
            if (description != null) step.set(DESCRIPTION, description)
            if (platform != null) step.set(PLATFORM, platform)
            if (agentId != null) step.set(AGENT_ID, agentId)
            if (apiUrl != null) step.set(API_URL, apiUrl)
            if (headers != null) step.set(HEADERS, headers)
            if (enabled != null) {
                step.set(ENABLED, enabled)
            }
            return step.where(ID.eq(id)).execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String): Int {
        with(TAiExternalAgentConfig.T_AI_EXTERNAL_AGENT_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }
}
