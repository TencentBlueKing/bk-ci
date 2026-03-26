package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AgentDao {
    fun getAgentByWorkspaceId(
        dslContext: DSLContext,
        projectId: String,
        workspaceId: String
    ): TEnvironmentThirdpartyAgentRecord? {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                .and(CREATE_WORKSPACE_NAME.eq(workspaceId.trim())).fetchAny()
        }
    }
}