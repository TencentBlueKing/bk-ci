package com.tencent.devops.misc.cron

import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.DEFAULT_SYTEM_USER
import com.tencent.devops.misc.dao.NodeDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UpdateAgentStatus @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpdateAgentStatus::class.java)
        private val LOCK_KEY = "env_cron_updateAgentStatus"
        private val LOCK_VALUE = "env_cron_updateAgentStatus"
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 15000)
    fun runUpdateAgentStatus() {
        logger.info("runUpdateAgentStatus")
        val lockValue = redisOperation.get(LOCK_KEY)
        if (lockValue != null) {
            logger.info("get lock failed, skip")
            return
        } else {
            redisOperation.set(
                LOCK_KEY,
                LOCK_VALUE, 30)
        }

        try {
            updateAgentStatus()
        } catch (t: Throwable) {
            logger.warn("update agent status failed", t)
        }
    }

    private fun updateAgentStatus() {
        val allServerNodes = nodeDao.listAllServerNodes(dslContext)

        if (allServerNodes.isEmpty()) {
            return
        }

        val allIps = allServerNodes.map { it.nodeIp }.toSet()
        val agentStatusMap = EsbAgentClient.getAgentStatus(DEFAULT_SYTEM_USER, allIps)

        allServerNodes.forEach {
            it.agentStatus = agentStatusMap[it.nodeIp] ?: false
        }

        nodeDao.batchUpdateNode(dslContext, allServerNodes)
    }
}