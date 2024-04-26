package com.tencent.devops.environment.dao.thirdpartyagent

import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgentAction
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentActionRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ThirdPartyAgentActionDao {
    fun addAgentAction(
        dslContext: DSLContext,
        projectId: String,
        agentId: Long,
        action: String
    ) {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                AGENT_ID,
                ACTION,
                ACTION_TIME
            ).values(
                projectId,
                agentId,
                action,
                LocalDateTime.now()
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
                .orderBy(ACTION_TIME.desc(), ID.desc()) // fix 时间相同时，增加ID排序
                .limit(offset, limit)
                .fetch()
        }
    }

    fun getAgentActionsCount(dslContext: DSLContext, projectId: String, agentId: Long): Long {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(AGENT_ID.eq(agentId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getAgentLastAction(dslContext: DSLContext, projectId: String, agentId: Long): String? {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            return dslContext.select(ACTION).from(this).where(PROJECT_ID.eq(projectId)).and(AGENT_ID.eq(agentId))
                .orderBy(ID.desc()).fetchAny()?.get(ACTION)
        }
    }
}