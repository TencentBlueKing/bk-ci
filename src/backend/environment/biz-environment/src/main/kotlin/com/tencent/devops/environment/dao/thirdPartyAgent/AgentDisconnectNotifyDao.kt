package com.tencent.devops.environment.dao.thirdPartyAgent

import com.tencent.devops.model.environment.tables.TAgentFailureNotifyUser
import com.tencent.devops.model.environment.tables.records.TAgentFailureNotifyUserRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class AgentDisconnectNotifyDao @Autowired constructor(private val dslContext: DSLContext) {

    fun list(): List<TAgentFailureNotifyUserRecord> {
        with(TAgentFailureNotifyUser.T_AGENT_FAILURE_NOTIFY_USER) {
            return dslContext.selectFrom(this)
                    .fetch()
        }
    }
}