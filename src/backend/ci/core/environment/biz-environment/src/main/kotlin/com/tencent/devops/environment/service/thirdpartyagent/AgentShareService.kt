package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.environment.dao.AgentShareProjectDao
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentShared
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentShareService @Autowired constructor(
    private val dslContext: DSLContext,
    private val agentShareProjectDao: AgentShareProjectDao
) {

    fun addShared(
        shares: AgentShared
    ): Boolean {
        if (shares.sharedProjectId.isEmpty()) {
            return false
        }
        if (shares.sharedProjectId.size > 1) {
            agentShareProjectDao.batchAdd(
                dslContext = dslContext,
                agentId = shares.agentId,
                mainProjectId = shares.mainProjectId,
                sharedProjectIds = shares.sharedProjectId,
                creator = shares.userId
            )
            return true
        }
        return agentShareProjectDao.add(
            dslContext = dslContext,
            agentId = shares.agentId,
            mainProjectId = shares.mainProjectId,
            sharedProjectId = shares.sharedProjectId.first(),
            creator = shares.userId
        ) > 0
    }

    fun deleteSharedAgent(
        shares: AgentShared
    ): Boolean {
        if (shares.sharedProjectId.isEmpty()) {
            return false
        }
        shares.sharedProjectId.forEach {
            agentShareProjectDao.delete(
                dslContext = dslContext,
                agentId = shares.agentId,
                mainProjectId = shares.mainProjectId,
                sharedProjectId = it
            )
        }
        return true
    }
}
