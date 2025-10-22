/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.misc.dao.environment

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgentAction
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class EnvironmentThirdPartyAgentDao {

    fun listByStatus(
        dslContext: DSLContext,
        status: Set<AgentStatus>
    ): List<TEnvironmentThirdpartyAgentRecord> {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(STATUS.`in`(status.map { it.status }))
                .fetch()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        nodeId: Long?,
        projectId: String,
        status: AgentStatus
    ): Int {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            val step = dslContext.update(this)
                .set(STATUS, status.status)
            if (nodeId != null) {
                step.set(NODE_ID, nodeId)
            }
            return step.where(ID.eq(id))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        nodeId: Long?,
        projectId: String,
        status: AgentStatus,
        expectStatus: AgentStatus
    ): Int {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            val step = dslContext.update(this)
                .set(STATUS, status.status)
            if (nodeId != null) {
                step.set(NODE_ID, nodeId)
            }
            return step.where(ID.eq(id))
                .and(PROJECT_ID.eq(projectId))
                .and(STATUS.eq(expectStatus.status))
                .execute()
        }
    }

    fun addAgentAction(
        dslContext: DSLContext,
        projectId: String,
        agentId: Long,
        action: String
    ) {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            dslContext.insertInto(this,
                PROJECT_ID,
                AGENT_ID,
                ACTION,
                ACTION_TIME)
                .values(
                    projectId,
                    agentId,
                    action,
                    LocalDateTime.now()
                ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long,
        projectId: String
    ): Int {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun getAgentsByNodeIds(
        dslContext: DSLContext,
        nodeIds: Collection<Long>,
        projectId: String
    ): Result<TEnvironmentThirdpartyAgentRecord> {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(NODE_ID.`in`(nodeIds))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }
}
