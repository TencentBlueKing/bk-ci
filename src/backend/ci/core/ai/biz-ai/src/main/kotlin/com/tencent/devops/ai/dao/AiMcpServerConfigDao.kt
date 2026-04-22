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

import com.tencent.devops.model.ai.tables.TAiMcpServerConfig
import com.tencent.devops.model.ai.tables.records.TAiMcpServerConfigRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/** MCP 服务端配置 DAO，对应 T_AI_MCP_SERVER_CONFIG 表。 */
@Repository
class AiMcpServerConfigDao {

    fun create(
        dslContext: DSLContext,
        id: String,
        scope: String,
        userId: String?,
        serverName: String,
        serverUrl: String,
        transportType: String,
        headers: String?,
        bindAgent: String,
        enabled: Boolean
    ) {
        val now = LocalDateTime.now()
        with(TAiMcpServerConfig.T_AI_MCP_SERVER_CONFIG) {
            dslContext.insertInto(
                this,
                ID, SCOPE, USER_ID, SERVER_NAME, SERVER_URL,
                TRANSPORT_TYPE, HEADERS, BIND_AGENT, ENABLED,
                CREATED_TIME, UPDATED_TIME
            ).values(
                id, scope, userId, serverName, serverUrl,
                transportType, headers, bindAgent,
                enabled, now, now
            ).execute()
        }
    }

    fun getById(
        dslContext: DSLContext,
        id: String
    ): TAiMcpServerConfigRecord? {
        with(TAiMcpServerConfig.T_AI_MCP_SERVER_CONFIG) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun listEnabled(
        dslContext: DSLContext,
        scope: String,
        bindAgent: String? = null
    ): Result<TAiMcpServerConfigRecord> {
        with(TAiMcpServerConfig.T_AI_MCP_SERVER_CONFIG) {
            val query = dslContext.selectFrom(this)
                .where(SCOPE.eq(scope))
                .and(ENABLED.eq(true))
            if (bindAgent != null) {
                query.and(BIND_AGENT.eq(bindAgent))
            }
            return query.orderBy(CREATED_TIME.asc()).fetch()
        }
    }

    fun listEnabledByUser(
        dslContext: DSLContext,
        userId: String,
        bindAgent: String? = null
    ): Result<TAiMcpServerConfigRecord> {
        with(TAiMcpServerConfig.T_AI_MCP_SERVER_CONFIG) {
            val query = dslContext.selectFrom(this)
                .where(SCOPE.eq("USER"))
                .and(USER_ID.eq(userId))
                .and(ENABLED.eq(true))
            if (bindAgent != null) {
                query.and(BIND_AGENT.eq(bindAgent))
            }
            return query.orderBy(CREATED_TIME.asc()).fetch()
        }
    }

    fun listByUser(
        dslContext: DSLContext,
        userId: String
    ): Result<TAiMcpServerConfigRecord> {
        with(TAiMcpServerConfig.T_AI_MCP_SERVER_CONFIG) {
            return dslContext.selectFrom(this)
                .where(SCOPE.eq("USER"))
                .and(USER_ID.eq(userId))
                .orderBy(CREATED_TIME.desc())
                .fetch()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: String,
        serverName: String?,
        serverUrl: String?,
        transportType: String?,
        headers: String?,
        bindAgent: String?,
        enabled: Boolean?
    ): Int {
        with(TAiMcpServerConfig.T_AI_MCP_SERVER_CONFIG) {
            val step = dslContext.update(this)
                .set(UPDATED_TIME, LocalDateTime.now())
            if (serverName != null) step.set(SERVER_NAME, serverName)
            if (serverUrl != null) step.set(SERVER_URL, serverUrl)
            if (transportType != null) step.set(TRANSPORT_TYPE, transportType)
            if (headers != null) step.set(HEADERS, headers)
            if (bindAgent != null) step.set(BIND_AGENT, bindAgent)
            if (enabled != null) step.set(ENABLED, enabled)
            return step.where(ID.eq(id)).execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String): Int {
        with(TAiMcpServerConfig.T_AI_MCP_SERVER_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }
}
