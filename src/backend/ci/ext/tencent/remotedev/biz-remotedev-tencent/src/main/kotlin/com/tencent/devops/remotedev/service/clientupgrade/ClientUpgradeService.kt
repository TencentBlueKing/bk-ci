package com.tencent.devops.remotedev.service.clientupgrade

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.records.TClientRecord
import com.tencent.devops.remotedev.dao.ClientDao
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
    private val clientDao: ClientDao,
    private val upgradeProps: UpgradeProps
) {
    fun checkUpgrade(userId: String, data: ClientUpgradeData): ClientUpgradeResp {
        val record =
            clientDao.fetchAny(dslContext, data.macAddress) ?: return ClientUpgradeResp.noUpgrade()
        if (!upgradeProps.checkCanUpgrade(record.macAddress)) {
            return ClientUpgradeResp.noUpgrade()
        }
        // 即使在列表中时还要校验下，保证实时性
        val currentClientVersion = upgradeProps.getClientVersion()
        val currentStartVersion = upgradeProps.getStartVersion()
        val dynamicProps = initUpgradeDynamicProps(currentClientVersion, currentStartVersion)
        val clientVersion = checkVersion(false, currentClientVersion, record, dynamicProps)
        val startVersion = checkVersion(true, currentStartVersion, record, dynamicProps)
        return ClientUpgradeResp(clientVersion, startVersion)
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

    private fun listCanUpgradeClients(maxParallelCount: Int): Set<String>? {
        val currentClientVersion = upgradeProps.getClientVersion()
        val currentStartVersion = upgradeProps.getStartVersion()
        if (currentClientVersion.isBlank() && currentStartVersion.isBlank()) {
            return null
        }

        val dynamicProps = initUpgradeDynamicProps(currentClientVersion, currentStartVersion)

        val canUpgradeMacAddressSet = mutableSetOf<String>()
        // 暂时先全量查询，后续看性能有没有用影响
        val records = clientDao.fetchAll(dslContext, LAST_REQUEST_BEFORE_DAYS)
        records.forEach {
            val clientCan = checkVersion(false, currentClientVersion, it, dynamicProps)
            val startCan = checkVersion(true, currentStartVersion, it, dynamicProps)
            if (!clientCan.isNullOrBlank() || !startCan.isNullOrBlank()) {
                canUpgradeMacAddressSet.add(it.macAddress)
            }
            if (canUpgradeMacAddressSet.size >= maxParallelCount) {
                return canUpgradeMacAddressSet
            }
        }

        return canUpgradeMacAddressSet
    }

    // 初始化一些动态的参数
    private fun initUpgradeDynamicProps(
        clientCurrentVersion: String,
        startCurrentVersion: String
    ): UpgradeDynamicProps {
        // 设置当前符合版本的个数
        val clientMaxNumber = upgradeProps.getClientMaxNumb()
        var clientCanUpgradeNumb: Int? = null
        if (clientMaxNumber != null && clientCurrentVersion.isNotBlank()) {
            val count = clientDao.fetchVersionCount(dslContext, clientCurrentVersion)
            clientCanUpgradeNumb = (clientMaxNumber - count).let { if (it < 0) 0 else it }
        }

        val startMaxNumber = upgradeProps.getStartMaxNumb()
        var startCanUpgradeNumb: Int? = null
        if (startMaxNumber != null && startCurrentVersion.isNotBlank()) {
            val count = clientDao.fetchStartVersionCount(dslContext, startCurrentVersion)
            startCanUpgradeNumb = (startMaxNumber - count).let { if (it < 0) 0 else it }
        }

        return UpgradeDynamicProps(
            clientCanUpgradeNumb = clientCanUpgradeNumb,
            startCanUpgradeNumb = startCanUpgradeNumb,
            clientUserVersion = upgradeProps.getClientUserVersion(),
            clientProjectVersion = upgradeProps.getClientProjectVersion(),
            startUserVersion = upgradeProps.getStartUserVersion(),
            startProjectVersion = upgradeProps.getStartProjectVersion()
        )
    }

    private fun checkVersion(
        isStart: Boolean,
        inCurrentVersion: String?,
        record: TClientRecord,
        props: UpgradeDynamicProps
    ): String? {
        // 为空的上报版本不参与比较
        val version = if (isStart) {
            record.startVersion
        } else {
            record.version
        }.trim().ifBlank { return null }

        // 根据用户升级版本
        val currentUser = record.currentUser
        val userVersion = props.userVersion(isStart)
        if (currentUser.isNotBlank() && userVersion.containsKey(currentUser)) {
            return if (version != userVersion[currentUser]?.trim()) {
                userVersion[currentUser]
            } else {
                null
            }
        }

        // 根据项目升级版本
        val projectId = record.projectId
        val projectVersion = props.projectVersion(isStart)
        if (projectId.isNotBlank() && projectVersion.containsKey(projectId)) {
            return if (version != projectVersion[projectId]?.trim()) {
                projectVersion[projectId]
            } else {
                null
            }
        }

        // 正常升级
        val currentVersion = (inCurrentVersion ?: if (isStart) {
            upgradeProps.getStartVersion()
        } else {
            upgradeProps.getClientVersion()
        }).trim().ifBlank { return null }
        if (version == currentVersion) {
            return null
        }
        val canUpgradeNumb = props.canUpgradeNumb(isStart) ?: return null
        if (canUpgradeNumb - 1 < 0) {
            return null
        }
        props.setCanUpgradeNumb(isStart, canUpgradeNumb - 1)
        return currentVersion
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientUpgradeService::class.java)
        private const val LOCK_KEY = "remotedev_cron_updateCanUpgradeClients"

        // 客户端最后一次请求的时间
        private const val LAST_REQUEST_BEFORE_DAYS = 14
    }
}

data class UpgradeDynamicProps(
    var clientCanUpgradeNumb: Int?,
    var startCanUpgradeNumb: Int?,
    val clientUserVersion: Map<String, String>,
    val clientProjectVersion: Map<String, String>,
    val startUserVersion: Map<String, String>,
    val startProjectVersion: Map<String, String>
) {
    fun canUpgradeNumb(isStart: Boolean) = if (isStart) startCanUpgradeNumb else clientCanUpgradeNumb
    fun setCanUpgradeNumb(isStart: Boolean, numb: Int) {
        if (isStart) {
            startCanUpgradeNumb = numb
        } else {
            clientCanUpgradeNumb = numb
        }
    }

    fun userVersion(isStart: Boolean) = if (isStart) startUserVersion else clientUserVersion
    fun projectVersion(isStart: Boolean) = if (isStart) startProjectVersion else clientProjectVersion
}