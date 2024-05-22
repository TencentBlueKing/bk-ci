package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentDisableInfo
import com.tencent.devops.environment.model.AgentDisableType
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.utils.ThirdAgentActionAddLock
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 对第三方构建机一些自身数据操作
 */
@Service
class ThirdPartAgentService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val agentActionDao: ThirdPartyAgentActionDao,
    private val agentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao
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

    fun disableAgent(
        projectIds: Set<String>?,
        agentAndNodeIds: Pair<Set<Long>, Set<Long>>?
    ) {
        if (projectIds.isNullOrEmpty() && agentAndNodeIds == null) {
            return
        }
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            if (!projectIds.isNullOrEmpty()) {
                agentDao.updateAgentByProject(
                    dslContext = context,
                    projectIds = projectIds,
                    agents = null,
                    status = AgentStatus.DISABLED,
                    disableInfo = AgentDisableInfo(
                        type = AgentDisableType.PROJECT_DISABLED, time = LocalDateTime.now().timestamp()
                    )
                )
            } else if (agentAndNodeIds != null) {
                agentDao.batchUpdateAgent(context, agentAndNodeIds.first, AgentStatus.DISABLED)
            }
            nodeDao.batchUpdateNodeStatus(context, projectIds, agentAndNodeIds?.second, NodeStatus.ABNORMAL)
        }
    }
}