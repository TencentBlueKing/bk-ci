package com.tencent.devops.misc.service

import com.tencent.devops.common.api.enum.AgentStatus
import com.tencent.devops.common.misc.AgentGrayUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.dao.ThirdPartyAgentDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UpgradeService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val agentGrayUtils: AgentGrayUtils,
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao
) {

    fun updateCanUpgradeAgentList() {
        val maxParallelCount = redisOperation.get(PARALLEL_UPGRADE_COUNT)?.toInt() ?: DEFAULT_PARALLEL_UPGRADE_COUNT
        if (maxParallelCount < 1) {
            logger.warn("parallel count set to zero")
            agentGrayUtils.setCanUpgradeAgents(listOf())
            return
        }

        val currentVersion = redisOperation.get(agentGrayUtils.getAgentVersionKey())
        val currentMasterVersion = redisOperation.get(agentGrayUtils.getAgentMasterVersionKey())
        if (currentMasterVersion.isNullOrBlank() || currentVersion.isNullOrBlank()) {
            logger.warn("invalid server agent version")
            return
        }

        val importOKAgents = thirdPartyAgentDao.listByStatus(dslContext, setOf(AgentStatus.IMPORT_OK))
        val needUpgradeAgents = importOKAgents.filter {
            if (it.version.isNullOrBlank() || it.masterVersion.isNullOrBlank()) { // 旧Agent不处理
                false
            } else {
                it.version != currentVersion || it.masterVersion != currentMasterVersion
            }
        }
        val canUpgraderAgent = if (needUpgradeAgents.size > maxParallelCount) {
            needUpgradeAgents.subList(0, maxParallelCount)
        } else {
            needUpgradeAgents
        }
        agentGrayUtils.setCanUpgradeAgents(canUpgraderAgent.map { it.id })
    }

    fun setMaxParallelUpgradeCount(count: Int) {
        redisOperation.set(PARALLEL_UPGRADE_COUNT, count.toString())
    }

    fun getMaxParallelUpgradeCount(): Int {
        return redisOperation.get(PARALLEL_UPGRADE_COUNT)?.toInt() ?: DEFAULT_PARALLEL_UPGRADE_COUNT
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UpgradeService::class.java)
        private const val PARALLEL_UPGRADE_COUNT = "environment.thirdparty.agent.parallel.upgrade.count"
        private const val DEFAULT_PARALLEL_UPGRADE_COUNT = 50
    }
}