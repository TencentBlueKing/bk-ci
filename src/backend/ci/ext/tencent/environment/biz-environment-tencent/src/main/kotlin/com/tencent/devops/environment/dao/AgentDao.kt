package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.imate.ImateListItem
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
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
}