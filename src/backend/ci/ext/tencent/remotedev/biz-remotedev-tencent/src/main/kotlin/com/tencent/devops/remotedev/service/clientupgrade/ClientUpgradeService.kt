package com.tencent.devops.remotedev.service.clientupgrade

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.records.TClientRecord
import com.tencent.devops.remotedev.dao.ClientDao
import com.tencent.devops.remotedev.pojo.ClientUpgradeComp
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeData
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeResp
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
        val record = clientDao.fetchAny(dslContext, data.macAddress) ?: return ClientUpgradeResp.noUpgrade()
        if (data.forceUpdate != true && !upgradeProps.checkCanUpgrade(record.macAddress)) {
            return ClientUpgradeResp.noUpgrade()
        }
        // 即使在列表中时还要校验下，保证实时性
        val os = OS.parse(record.os) ?: return ClientUpgradeResp.noUpgrade()
        val currentClientVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.CLIENT, os)
        val currentStartVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.START, os)
        if (currentClientVersion.isBlank() && currentStartVersion.isBlank()) {
            return ClientUpgradeResp.noUpgrade()
        }
        val dynamicProps = initUpgradeDynamicProps(
            os = os,
            clientCurrentVersion = currentClientVersion,
            startCurrentVersion = currentStartVersion,
            noMax = data.forceUpdate ?: false
        )
        val clientVersion = checkVersion(
            upgradeComp = ClientUpgradeComp.CLIENT,
            inCurrentVersion = currentClientVersion,
            record = record,
            props = dynamicProps,
            noMax = data.forceUpdate ?: false
        )
        val startVersion = checkVersion(
            upgradeComp = ClientUpgradeComp.START,
            inCurrentVersion = currentStartVersion,
            record = record,
            props = dynamicProps,
            noMax = data.forceUpdate ?: false
        )
        return ClientUpgradeResp(clientVersion, startVersion)
    }

    @Scheduled(initialDelay = 5 * 60 * 1000L, fixedDelay = 6 * 60 * 1000L)
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
            val canUpgradeClients = listCanUpgradeClients(maxParallelCount).ifEmpty { return }

            watcher.start("setCanUpgradeClients")
            upgradeProps.setCanUpgradeClients(canUpgradeClients)
        } catch (ignore: Throwable) {
            logger.warn("updateCanUpgradeClients failed", ignore)
        } finally {
            lock.unlock()
            logger.info("updateCanUpgradeClients| $watcher")
        }
    }

    private fun listCanUpgradeClients(maxParallelCount: Int): Set<String> {
        val canUpgradeMacAddressSet = mutableSetOf<String>()
        // 暂时先全量查询，后续看性能有没有用影响
        val records = clientDao.fetchAll(dslContext, LAST_REQUEST_BEFORE_DAYS)
        records.forEach {
            val os = OS.parse(it.os) ?: return@forEach
            val clientCurrentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.CLIENT, os)
            val startCurrentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.START, os)
            if (clientCurrentVersion.isBlank() && startCurrentVersion.isBlank()) {
                return@forEach
            }
            val dynamicProps = initUpgradeDynamicProps(
                os = os,
                clientCurrentVersion = clientCurrentVersion,
                startCurrentVersion = startCurrentVersion,
                noMax = false
            )
            val clientCan = checkVersion(
                upgradeComp = ClientUpgradeComp.CLIENT,
                inCurrentVersion = clientCurrentVersion,
                record = it,
                props = dynamicProps,
                noMax = false
            )
            val startCan = checkVersion(
                upgradeComp = ClientUpgradeComp.START,
                inCurrentVersion = startCurrentVersion,
                record = it,
                props = dynamicProps,
                noMax = false
            )
            if (!clientCan.isNullOrBlank() || !startCan.isNullOrBlank()) {
                canUpgradeMacAddressSet.add(it.macAddress)
            }
            if (canUpgradeMacAddressSet.size >= maxParallelCount) {
                return canUpgradeMacAddressSet
            }
        }

        return canUpgradeMacAddressSet
    }

    /**
     * 初始化一些动态的参数
     * @param noMax 不管最大升级数的限制
     */
    private fun initUpgradeDynamicProps(
        os: OS,
        clientCurrentVersion: String,
        startCurrentVersion: String,
        noMax: Boolean
    ): UpgradeDynamicProps {
        // 设置当前符合版本的个数
        val clientMaxNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.CLIENT, os)
        var clientCanUpgradeNumb: Int? = null
        if (!noMax && clientCurrentVersion.isNotBlank()) {
            val count = clientDao.fetchVersionCount(dslContext, clientCurrentVersion)
            clientCanUpgradeNumb = (clientMaxNumber - count).let { if (it < 0) 0 else it }
        }

        val startMaxNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.START, os)
        var startCanUpgradeNumb: Int? = null
        if (!noMax && startCurrentVersion.isNotBlank()) {
            val count = clientDao.fetchStartVersionCount(dslContext, startCurrentVersion)
            startCanUpgradeNumb = (startMaxNumber - count).let { if (it < 0) 0 else it }
        }

        return UpgradeDynamicProps(
            clientCanUpgradeNumb = clientCanUpgradeNumb,
            clientUserVersion = upgradeProps.getUserVersion(ClientUpgradeComp.CLIENT, os),
            clientWorkspaceNameVersion = upgradeProps.getWorkspaceNameVersion(ClientUpgradeComp.CLIENT, os),
            clientProjectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.CLIENT, os),
            startCanUpgradeNumb = startCanUpgradeNumb,
            startUserVersion = upgradeProps.getUserVersion(ClientUpgradeComp.START, os),
            startWorkspaceNameVersion = upgradeProps.getWorkspaceNameVersion(ClientUpgradeComp.START, os),
            startProjectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.START, os)
        )
    }

    @Suppress("ComplexMethod")
    private fun checkVersion(
        upgradeComp: ClientUpgradeComp,
        inCurrentVersion: String?,
        record: TClientRecord,
        props: UpgradeDynamicProps,
        noMax: Boolean
    ): String? {
        // 为空的上报版本不参与比较，start除外
        val version = when (upgradeComp) {
            ClientUpgradeComp.START -> record.startVersion
            ClientUpgradeComp.CLIENT -> record.version
        }.trim()
        if (version.isBlank() && upgradeComp == ClientUpgradeComp.CLIENT) {
            return null
        }

        // 根据用户升级版本
        val currentUser = record.currentUser
        val userVersion = props.userVersion(upgradeComp)
        if (currentUser.isNotBlank() && userVersion.containsKey(currentUser)) {
            return if (version != userVersion[currentUser]?.trim()) {
                userVersion[currentUser]
            } else {
                null
            }
        }

        // 根据工作空间升级
        val currentWorkspaceNames = JsonUtil.to(
            record.currentWorkspaceNames.data(), object : TypeReference<Set<String>>() {}
        )
        val workspaceVersion = props.workspaceNames(upgradeComp)
        currentWorkspaceNames.forEach { workspaceName ->
            if (workspaceVersion.containsKey(workspaceName)) {
                return if (version != workspaceVersion[workspaceName]?.trim()) {
                    workspaceVersion[workspaceName]
                } else {
                    null
                }
            }
        }

        // 根据项目升级版本
        val currentProjectIds = JsonUtil.to(record.currentProjectIds.data(), object : TypeReference<Set<String>>() {})
        val projectVersion = props.projectVersion(upgradeComp)
        currentProjectIds.forEach { projectId ->
            if (projectVersion.containsKey(projectId)) {
                return if (version != projectVersion[projectId]?.trim()) {
                    projectVersion[projectId]
                } else {
                    null
                }
            }
        }

        // 正常升级
        val currentVersion = (inCurrentVersion ?: (upgradeProps.getCurrentVersion(
            comp = upgradeComp,
            os = OS.parse(record.os) ?: return null
        ))).trim().ifBlank { return null }
        if (version == currentVersion) {
            return null
        }

        // 正常升级检查最大升级数
        if (!noMax) {
            val canUpgradeNumb = props.canUpgradeNumb(upgradeComp) ?: return null
            if (canUpgradeNumb - 1 < 0) {
                return null
            }
            props.setCanUpgradeNumb(upgradeComp, canUpgradeNumb - 1)
        }

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
    val clientUserVersion: Map<String, String>,
    val clientWorkspaceNameVersion: Map<String, String>,
    val clientProjectVersion: Map<String, String>,
    var startCanUpgradeNumb: Int?,
    val startUserVersion: Map<String, String>,
    val startWorkspaceNameVersion: Map<String, String>,
    val startProjectVersion: Map<String, String>
) {
    fun canUpgradeNumb(upgradeComp: ClientUpgradeComp) = when (upgradeComp) {
        ClientUpgradeComp.START -> startCanUpgradeNumb
        ClientUpgradeComp.CLIENT -> clientCanUpgradeNumb
    }

    fun setCanUpgradeNumb(upgradeComp: ClientUpgradeComp, numb: Int) {
        when (upgradeComp) {
            ClientUpgradeComp.START -> startCanUpgradeNumb = numb
            ClientUpgradeComp.CLIENT -> clientCanUpgradeNumb = numb
        }
    }

    fun userVersion(upgradeComp: ClientUpgradeComp) = when (upgradeComp) {
        ClientUpgradeComp.START -> startUserVersion
        ClientUpgradeComp.CLIENT -> clientUserVersion
    }

    fun projectVersion(upgradeComp: ClientUpgradeComp) = when (upgradeComp) {
        ClientUpgradeComp.START -> startProjectVersion
        ClientUpgradeComp.CLIENT -> clientProjectVersion
    }

    fun workspaceNames(upgradeComp: ClientUpgradeComp) = when (upgradeComp) {
        ClientUpgradeComp.START -> startWorkspaceNameVersion
        ClientUpgradeComp.CLIENT -> clientWorkspaceNameVersion
    }
}
