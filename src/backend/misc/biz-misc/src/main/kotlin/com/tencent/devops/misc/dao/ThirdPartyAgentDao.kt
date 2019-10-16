package com.tencent.devops.misc.dao

import com.tencent.devops.common.api.enum.AgentStatus
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgentAction
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ThirdPartyAgentDao {

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