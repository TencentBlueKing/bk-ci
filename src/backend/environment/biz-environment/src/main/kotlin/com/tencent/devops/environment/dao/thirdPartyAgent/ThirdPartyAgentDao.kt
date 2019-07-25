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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.dao.thirdPartyAgent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgentAction
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentActionRecord
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class ThirdPartyAgentDao {

    fun add(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        os: OS,
        secretKey: String,
        gateway: String?
    ): Long {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                OS,
                STATUS,
                SECRET_KEY,
                CREATED_USER,
                CREATED_TIME,
                GATEWAY
            ).values(
                projectId,
                os.name,
                AgentStatus.UN_IMPORT.status,
                secretKey,
                userId,
                LocalDateTime.now(),
                gateway ?: ""
            )
                .returning(ID)
                .fetchOne().id
        }
    }

    fun updateGateway(dslContext: DSLContext, agentId: Long, gateway: String) {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            dslContext.update(this)
                .set(GATEWAY, gateway)
                .where(ID.eq(agentId))
                .execute()
        }
    }

    fun listUnimportAgent(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        os: OS
    ): Result<TEnvironmentThirdpartyAgentRecord> {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(STATUS.`in`(AgentStatus.UN_IMPORT.status, AgentStatus.UN_IMPORT_OK.status))
                .and(PROJECT_ID.eq(projectId))
                .and(CREATED_USER.eq(userId))
                .and(OS.eq(os.name))
                .orderBy(CREATED_TIME.desc())
                .fetch()
        }
    }

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

    fun countAgentByStatusAndOS(
        dslContext: DSLContext,
        projectId: String,
        nodeIds: Set<Long>,
        status: AgentStatus,
        os: OS
    ): Int {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.`in`(nodeIds))
                .and(STATUS.eq(status.status))
                .and(OS.eq(os.name))
                .fetchOne(0, Int::class.java)
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

    fun updateAgentVersion(
        dslContext: DSLContext,
        id: Long,
        projectId: String,
        version: String,
        masterVersion: String
    ): Int {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            val step = dslContext.update(this)
                .set(VERSION, version)
                .set(MASTER_VERSION, masterVersion)
            return step.where(ID.eq(id))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun saveAgent(
        dslContext: DSLContext,
        agentRecode: TEnvironmentThirdpartyAgentRecord
    ) {
        dslContext.executeUpdate(agentRecode)
    }

    fun updateAgentInfo(
        dslContext: DSLContext,
        id: Long,
        remoteIp: String,
        projectId: String,
        hostname: String,
        ip: String,
        detectOS: String,
        agentVersion: String?,
        masterVersion: String?
    ): Int {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
                val agentRecord = context.selectFrom(this)
                    .where(ID.eq(id))
                    .forUpdate()
                    .fetchOne() ?: throw NotFoundException("The agent is not exist")

                val agentStatus = AgentStatus.fromStatus(agentRecord.status)

                if (AgentStatus.isDelete(agentStatus)) {
                    // The agent already delete
                    throw NotFoundException("The agent is already deleted")
                }

                val step = context.update(this)
                    .set(HOSTNAME, hostname)
                    .set(IP, ip)
                    .set(DETECT_OS, detectOS)
                    .set(START_REMOTE_IP, remoteIp)

                if (agentVersion.isNullOrBlank()) {
                    step.set(VERSION, "")
                } else {
                    step.set(VERSION, agentVersion)
                }

                if (masterVersion.isNullOrBlank()) {
                    step.set(MASTER_VERSION, "")
                } else {
                    step.set(MASTER_VERSION, masterVersion)
                }

                when {
                    AgentStatus.isUnImport(agentStatus) ->
                        step.set(STATUS, AgentStatus.UN_IMPORT_OK.status)
                    AgentStatus.isImportException(agentStatus) ->
                        step.set(STATUS, AgentStatus.IMPORT_OK.status)
                }

                step.where(ID.eq(id))
                    .and(PROJECT_ID.eq(projectId))
                    .execute()
            }
        }
    }

    fun getAgent(
        dslContext: DSLContext,
        id: Long,
        projectId: String
    ): TEnvironmentThirdpartyAgentRecord? {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .and(PROJECT_ID.eq(projectId))
                .forUpdate()
                .fetchOne()
        }
    }

    fun getAgentByNodeId(
        dslContext: DSLContext,
        nodeId: Long,
        projectId: String
    ): TEnvironmentThirdpartyAgentRecord? {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(NODE_ID.eq(nodeId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
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

    fun listImportAgent(
        dslContext: DSLContext,
        projectId: String,
        os: OS
    ): List<TEnvironmentThirdpartyAgentRecord> {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(OS.eq(os.name))
                .fetch()
        }
    }

    fun getAgent(
        dslContext: DSLContext,
        id: Long
    ): TEnvironmentThirdpartyAgentRecord? {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun saveAgentEnvs(dslContext: DSLContext, agentId: Long, envStr: String) {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            dslContext.update(this)
                .set(AGENT_ENVS, envStr)
                .where(ID.eq(agentId))
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
                    java.time.LocalDateTime.now()
                ).execute()
        }
    }

    fun listAgentActions(
        dslContext: DSLContext,
        projectId: String,
        agentId: Long,
        offset: Int,
        limit: Int
    ): Result<TEnvironmentThirdpartyAgentActionRecord> {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(AGENT_ID.eq(agentId))
                .orderBy(ACTION_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun getAgentActionsCount(dslContext: DSLContext, projectId: String, agentId: Long): Long {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(AGENT_ID.eq(agentId))
                .fetchOne(0, kotlin.Long::class.java)
        }
    }
}