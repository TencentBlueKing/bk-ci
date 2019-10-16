package com.tencent.devops.misc.cron

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.service.UpgradeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AgentUpgrdeJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val updateService: UpgradeService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AgentUpgrdeJob::class.java)
        private const val LOCK_KEY = "env_cron_updateCanUpgradeAgentList"
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 15000)
    fun updateCanUpgradeAgentList() {
        logger.info("updateCanUpgradeAgentList")
        val lockValue = redisOperation.get(LOCK_KEY)
        if (lockValue != null) {
            logger.info("get lock failed, skip")
            return
        } else {
            redisOperation.set(LOCK_KEY, "LOCKED", 60)
        }

        try {
            updateService.updateCanUpgradeAgentList()
        } catch (t: Throwable) {
            logger.warn("update can upgrade agent list failed", t)
        }
    }
}