package com.tencent.devops.remotedev.service.clientupgrade

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.records.TClientVersionRecord
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
    private val clientVersionDao: ClientVersionDao,
    private val upgradeProps: UpgradeProps
) {
    fun checkUpgrade(userId: String, data: ClientUpgradeData): ClientUpgradeResp {
        val record =
            clientVersionDao.fetchByMacAddress(dslContext, data.macAddress) ?: return ClientUpgradeResp.noUpgrade()
        if (upgradeProps.checkCanUpgrade(record.macAddress)) {
            // 即使在列表中时还要校验下，保证实时性
            val (clientCan, startCan) = checkCanUpgrade(record)
            return ClientUpgradeResp(clientCan, startCan)
        }
        return ClientUpgradeResp.noUpgrade()
    }

    @Scheduled(initialDelay = 10 * 1000L, fixedDelay = 30 * 1000L)
    fun updateCanUpgradeClients() {
        val watcher = Watcher("updateCanUpgradeClients")
        logger.debug("updateCanUpgradeClients start")
        watcher.start("try lock")
        val lock = RedisLock(redisOperation, lockKey = LOCK_KEY, expiredTimeInSeconds = 10 * 60L)
        try {
            if (!lock.tryLock()) {
                logger.debug("get lock failed, skip")
                return
            }
            watcher.start("get maxParallelCount")
            val maxParallelCount = upgradeProps.getMaxParallelUpgradeCount()
            if (maxParallelCount < 1) {
                logger.debug("parallel count set to zero")
                upgradeProps.setCanUpgradeClients(setOf())
                return
            }

            watcher.start("listCanUpdateClients")
            val canUpgradeClients = listCanUpgradeClients(maxParallelCount)?.ifEmpty { return } ?: return
            watcher.start("setCanUpgradeClients")
            upgradeProps.setCanUpgradeClients(canUpgradeClients)
        } catch (ignore: Throwable) {
            logger.warn("updateCanUpgradeClients failed", ignore)
        } finally {
            lock.unlock()
            logger.info("updateCanUpgradeClients| $watcher")
        }
    }

    fun listCanUpgradeClients(maxParallelCount: Int): Set<String>? {
        val currentClientVersion = upgradeProps.getClientVersion()
        val currentStartVersion = upgradeProps.getStartVersion()
        if (currentClientVersion.isBlank() && currentStartVersion.isBlank()) {
            return null
        }

        val canUpgradeMacAddressSet = mutableSetOf<String>()
        // TODO: 是否有可用和不可用的概念
        // TODO: 项目，start版本和当前使用用户也需要加到表中，看能不能从表中直接查出来
        val records = clientVersionDao.fetchUpgrade(dslContext, maxParallelCount)
        records.forEach {
            val (clientCan, startCan) = checkCanUpgrade(it, currentClientVersion, currentStartVersion)
            if (clientCan || startCan) {
                canUpgradeMacAddressSet.add(it.macAddress)
            }
        }

        return canUpgradeMacAddressSet
    }

    fun checkCanUpgrade(
        record: TClientVersionRecord,
        inCurrentClientVersion: String? = null,
        inCurrentStartVersion: String? = null
    ): Pair<Boolean, Boolean> {
        // 只有前面的限制条件都符合才能进入
        // TODO: 项目，start版本和当前使用用户对比

        val currentClientVersion = inCurrentClientVersion ?: upgradeProps.getClientVersion()
        val currentStartVersion = inCurrentStartVersion ?: upgradeProps.getStartVersion()
        return Pair(
            currentClientVersion.isNotBlank() && record.version != currentClientVersion,
            // TODO start的
            false
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientUpgradeService::class.java)
        private const val LOCK_KEY = "remotedev_cron_updateCanUpgradeClients"
    }
}