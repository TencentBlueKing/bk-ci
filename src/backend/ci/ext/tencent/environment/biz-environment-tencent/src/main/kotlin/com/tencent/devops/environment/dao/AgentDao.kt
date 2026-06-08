package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class AgentDao {
    fun getAgentByWorkspaceIdGlobal(
        dslContext: DSLContext,
        workspaceId: String,
        projectId: String?
    ): TEnvironmentThirdpartyAgentRecord? {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            val dsl = dslContext.selectFrom(this)
                .where(CREATE_WORKSPACE_NAME.eq(workspaceId.trim()))
            if (projectId != null) {
                return dsl.and(PROJECT_ID.eq(projectId)).fetchAny()
            }
            return dsl.fetchAny()
        }
    }

    fun deleteAgentByWorkspaceIdGlobal(dslContext: DSLContext, workspaceId: String) {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            dslContext.deleteFrom(this)
                .where(CREATE_WORKSPACE_NAME.eq(workspaceId.trim())).execute()
        }
    }

    fun fetchImateAgents(dslContext: DSLContext): List<TEnvironmentThirdpartyAgentRecord> {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this).where(CREATE_WORKSPACE_NAME.isNotNull).and(AGENT_PROPS.isNotNull)
                .and(
                    DSL.condition(
                        "JSON_CONTAINS({0}, '\"DEVCLOUD\"', '$.source')",
                        AGENT_PROPS
                    )
                ).fetch()
        }
    }

    fun batchDeleteAgents(dslContext: DSLContext, projectId: String?, agentIds: Set<Long>) {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(ID.`in`(agentIds)).execute()
        }
    }

    fun batchUpdateNodeDisplayName(dslContext: DSLContext, projectId: String?, nodeAndName: Map<Long, String>) {
        dslContext.batch(
            with(TNode.T_NODE) {
                nodeAndName.map { (nodeId, displayName) ->
                    dslContext.update(this).set(DISPLAY_NAME, displayName).where(NODE_ID.eq(nodeId))
                        .and(PROJECT_ID.eq(projectId))
                }
            }
        ).execute()
    }
}