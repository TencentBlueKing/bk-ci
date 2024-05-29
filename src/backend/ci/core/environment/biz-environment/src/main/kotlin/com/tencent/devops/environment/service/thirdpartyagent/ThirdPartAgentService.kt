package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import com.tencent.devops.environment.utils.ThirdAgentActionAddLock
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 对第三方构建机一些自身数据操作
 */
@Service
class ThirdPartAgentService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val agentActionDao: ThirdPartyAgentActionDao
) {
    fun addAgentAction(
        projectId: String,
        agentId: Long,
        action: AgentAction
    ) {
        val lock = ThirdAgentActionAddLock(redisOperation, projectId, agentId)
        if (agentActionDao.getAgentLastAction(dslContext, projectId, agentId) == action.name) {
            return
        }
        try {
            lock.lock()
            if (agentActionDao.getAgentLastAction(dslContext, projectId, agentId) == action.name) {
                return
            }
            agentActionDao.addAgentAction(
                dslContext = dslContext,
                projectId = projectId,
                agentId = agentId,
                action = action.name
            )
        } finally {
            lock.unlock()
        }
    }
}