package com.tencent.devops.environment.dao.thirdpartyagent

import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgentAction
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class ThirdPartyAgentActionDao {
    fun fetchAgentIdByGtCount(dslContext: DSLContext, size: Int): List<Long> {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            return dslContext.select(AGENT_ID).from(this).groupBy(AGENT_ID).having(DSL.count().greaterThan(size))
                .map { it[AGENT_ID] as Long }
        }
    }

    fun getIndexId(dslContext: DSLContext, agentId: Long, limit: Int, offset: Int): Long? {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            return dslContext.select(ID).from(this).where(AGENT_ID.eq(agentId))
                .orderBy(ID.desc()).offset(offset).limit(limit).fetchAny()?.get(ID)
        }
    }

    fun deleteOldActionById(dslContext: DSLContext, agentId: Long, id: Long): Int {
        with(TEnvironmentThirdpartyAgentAction.T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) {
            return dslContext.deleteFrom(this).where(AGENT_ID.eq(agentId)).and(ID.lessThan(id)).execute()
        }
    }
}
