package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.dao.ClientVersionDao
import com.tencent.devops.remotedev.pojo.ClientUpgradeData
import com.tencent.devops.remotedev.pojo.ClientUpgradeResp
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ClientUpgradeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val clientVersionDao: ClientVersionDao
) {
    fun checkUpgrade(userId: String, data: ClientUpgradeData): ClientUpgradeResp {
        val record =
            clientVersionDao.fetchByMacAddress(dslContext, data.macAddress) ?: return ClientUpgradeResp.noUpgrade()
    }

    @Scheduled(initialDelay = 10 * 1000L, fixedDelay = 30 * 1000L)
    fun updateCanUpgradeClients() {
        val watcher = Watcher("updateCanUpgradeClients")
        logger.debug("updateCanUpgradeClients start")
        watcher.start("try lock")
        val lock = RedisLock(redisOperation, lockKey = LOCK_KEY, expiredTimeInSeconds = MINUTES_10)
        try {
            if (!lock.tryLock()) {
                logger.debug("get lock failed, skip")
                return
            }
            watcher.start("get maxParallelCount")
            val maxParallelCount = agentPropsScope.getMaxParallelUpgradeCount()
            if (maxParallelCount < 1) {
                logger.debug("parallel count set to zero")
                agentScope.setCanUpgradeAgents(listOf())
                return
            }

            watcher.start("listCanUpdateClients")
            val canUpgradeAgents = listCanUpdateAgents(maxParallelCount) ?: return

            if (canUpgradeAgents.isNotEmpty()) {
                watcher.start("setCanUpgradeClients")
                agentScope.setCanUpgradeAgents(canUpgradeAgents.map { it.id })
            }
        } catch (ignore: Throwable) {
            logger.warn("updateCanUpgradeClients failed", ignore)
        } finally {
            lock.unlock()
            logger.info("updateCanUpgradeClients| $watcher")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientUpgradeService::class.java)
        private const val LOCK_KEY = "remotedev_cron_updateCanUpgradeClients"
    }
}