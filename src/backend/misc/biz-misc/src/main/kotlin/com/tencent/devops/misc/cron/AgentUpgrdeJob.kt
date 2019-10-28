package com.tencent.devops.misc.cron

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.service.AgentUpgradeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AgentUpgrdeJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val updateService: AgentUpgradeService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AgentUpgrdeJob::class.java)
        private const val LOCK_KEY = "env_cron_updateCanUpgradeAgentList"
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 15000)
    fun updateCanUpgradeAgentList() {
        logger.info("updateCanUpgradeAgentList")
        val lock = RedisLock(redisOperation, LOCK_KEY, 60)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            updateService.updateCanUpgradeAgentList()
        } catch (t: Throwable) {
            logger.warn("update can upgrade agent list failed", t)
        } finally {
            lock.unlock()
        }
    }
}