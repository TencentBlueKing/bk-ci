package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AgentDao {
    fun getAgentByWorkspaceIdGlobal(
        dslContext: DSLContext,
        workspaceId: String
    ): TEnvironmentThirdpartyAgentRecord? {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(CREATE_WORKSPACE_NAME.eq(workspaceId.trim())).fetchAny()
        }
    }

    fun fetchAgentsByWorkspaceIdGlobal(
        dslContext: DSLContext,
        workspaceIds: List<String>,
        projectId: String?
    ): List<TEnvironmentThirdpartyAgentRecord> {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            val dsl = dslContext.selectFrom(this)
                .where(CREATE_WORKSPACE_NAME.`in`(workspaceIds))
            if (projectId != null) {
                return dsl.and(PROJECT_ID.eq(projectId)).fetch()
            }
            return dsl.fetch()
        }
    }

    fun deleteAgentByWorkspaceIdGlobal(dslContext: DSLContext, workspaceId: String) {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            dslContext.deleteFrom(this)
                .where(CREATE_WORKSPACE_NAME.eq(workspaceId.trim())).execute()
        }
    }
}