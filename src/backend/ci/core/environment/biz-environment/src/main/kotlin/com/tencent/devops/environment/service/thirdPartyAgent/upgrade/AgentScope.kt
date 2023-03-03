package com.tencent.devops.environment.service.thirdPartyAgent.upgrade

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.environment.agent.AgentUpgradeType
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Agent升级相关配置和操作
 *
 * 检查Agent是否被锁定升级[checkLockUpgrade] 第1优先级
 * 检查Agent是否要强制升级[checkForceUpgrade] 第2优先级
 * 检查Agent是否安排升级[checkCanUpgrade] 第3优先级
 * 设置安排升级的AgentID[setCanUpgradeAgents]
 */
@Component
class AgentScope @Autowired constructor(private val redisOperation: RedisOperation) {

    fun checkForceUpgrade(agentId: String, type: AgentUpgradeType?): Boolean {
        return loadIdCache(getForceKeyByType(type)).contains(HashUtil.decodeIdToLong(agentId))
    }

    fun setUpgradeAgents(ids: List<Long>, upgradeKey: UpgradeKey, type: AgentUpgradeType?): Result<Boolean> {
        val cacheKey = getKeyType(upgradeKey, type)
        val failList = mutableSetOf<Long>()
        ids.forEach {
            if (!redisOperation.addSetValue(cacheKey, it.toString())) {
                failList.add(it)
            }
        }

        if (failList.size < ids.size) {
            invalidateIdCache(cacheKey)
        }

        if (failList.isNotEmpty()) {
            return Result(data = false, message = "fail list: $failList")
        }
        return success
    }

    private fun getKeyType(upgradeKey: UpgradeKey, type: AgentUpgradeType?): String =
        when (upgradeKey) {
            UpgradeKey.LOCK_UPGRADE -> getLockKeyByType(type) // 锁定升级
            UpgradeKey.FORCE_UPGRADE -> getForceKeyByType(type) // 强制升级
            UpgradeKey.CAN_UPGRADE -> getCanUpgradeAgentSetKey() // 是否允许升级
        }

    fun unsetUpgradeAgents(ids: List<Long>, upgradeKey: UpgradeKey, type: AgentUpgradeType?): Result<Boolean> {
        val cacheKey = getKeyType(upgradeKey, type)
        val failList = mutableSetOf<Long>()
        ids.forEach {
            if (!redisOperation.removeSetMember(cacheKey, it.toString())) {
                failList.add(it)
            }
        }
        if (failList.size < ids.size) {
            invalidateIdCache(cacheKey)
        }

        if (failList.isNotEmpty()) {
            return Result(data = false, message = "fail list: $failList")
        }
        return success
    }

    fun getAllUpgradeAgents(upgradeKey: UpgradeKey, type: AgentUpgradeType?): Set<Long> {
        return loadIdCache(getKeyType(upgradeKey, type))
    }

    fun cleanAllUpgradeAgents(upgradeKey: UpgradeKey, type: AgentUpgradeType?): Boolean {
        val cacheKey = getKeyType(upgradeKey, type)
        val delete = redisOperation.delete(cacheKey)
        invalidateIdCache(cacheKey)
        return delete
    }

    private fun getForceKeyByType(type: AgentUpgradeType?): String {
        if (type == null) {
            return FORCE_UPGRADE_AGENT_SET_KEY
        }
        return when (type) {
            AgentUpgradeType.WORKER -> FORCE_UPGRADE_AGENT_WORKER_SET_KEY
            AgentUpgradeType.GO_AGENT -> FORCE_UPGRADE_AGENT_GO_SET_KEY
            AgentUpgradeType.JDK -> FORCE_UPGRADE_AGENT_JDK_SET_KEY
            AgentUpgradeType.DOCKER_INIT_FILE -> FORCE_UPGRADE_AGENT_DOCKER_INIT_FILE_SET_KEY
        }
    }

    fun checkLockUpgrade(agentId: String, type: AgentUpgradeType?): Boolean {
        return loadIdCache(getLockKeyByType(type)).contains(HashUtil.decodeIdToLong(agentId))
    }

    private fun getLockKeyByType(type: AgentUpgradeType?): String {
        if (type == null) {
            return LOCK_UPGRADE_AGENT_SET_KEY
        }
        return when (type) {
            AgentUpgradeType.WORKER -> LOCK_UPGRADE_AGENT_WORKER_SET_KEY
            AgentUpgradeType.GO_AGENT -> LOCK_UPGRADE_AGENT_GO_SET_KEY
            AgentUpgradeType.JDK -> LOCK_UPGRADE_AGENT_JDK_SET_KEY
            AgentUpgradeType.DOCKER_INIT_FILE -> LOCK_UPGRADE_DOCKER_INIT_FILE_SET_KEY
        }
    }

    fun setCanUpgradeAgents(ids: List<Long>) {
        if (logger.isDebugEnabled) {
            logger.debug("setCanUpgradeAgents, ids: $ids")
        }
        var change = false
        val cacheKey = getCanUpgradeAgentSetKey()
        val existingAgentIds = loadIdCache(cacheKey, isDistinguishCluster = true)
        val newAgentIds = ids.toSet()
        val toAddAgentIds = newAgentIds.filterNot { existingAgentIds.contains(it) }
        if (toAddAgentIds.isNotEmpty()) {
            toAddAgentIds.forEach {
                redisOperation.addSetValue(key = cacheKey, item = it.toString(), isDistinguishCluster = true)
            }
            change = true
        }
        val toDeleteAgentIds = existingAgentIds.filterNot { newAgentIds.contains(it) }
        if (toDeleteAgentIds.isNotEmpty()) {
            toDeleteAgentIds.forEach {
                redisOperation.removeSetMember(key = cacheKey, item = it.toString(), isDistinguishCluster = true)
            }
            change = true
        }

        if (logger.isDebugEnabled) {
            logger.debug("$change|toAddIds=$toAddAgentIds|existIds=$existingAgentIds|toDeleteIds=$toDeleteAgentIds")
        }

        if (change) {
            invalidateIdCache(cacheKey)
        }
    }

    private fun getCanUpgradeAgentSetKey(): String {
        return CAN_UPGRADE_AGENT_SET_KEY
    }

    fun checkCanUpgrade(agentId: String): Boolean {
        return loadIdCache(getCanUpgradeAgentSetKey(), isDistinguishCluster = true)
            .contains(HashUtil.decodeIdToLong(agentId))
    }

    private val idCache: Cache<String, Set<Long>> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build()

    private fun invalidateIdCache(cacheKey: String) {
        idCache.invalidate(cacheKey)
    }

    private fun loadIdCache(cacheKey: String, isDistinguishCluster: Boolean = false): Set<Long> {
        return idCache.get(cacheKey) {
            redisOperation.getSetMembers(cacheKey, isDistinguishCluster)
                ?.filter { it.isNotBlank() }
                ?.map { it.toLong() }
                ?.toSet()
                ?: setOf()
        } ?: setOf()
    }

    companion object {
        private const val CACHE_EXPIRE_MIN = 1L
        private const val CACHE_SIZE = 16L
        private const val CAN_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:can_upgrade"

        private const val LOCK_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:lock_upgrade"

        private const val LOCK_UPGRADE_AGENT_WORKER_SET_KEY = "environment:thirdparty:worker:lock_upgrade"
        private const val LOCK_UPGRADE_AGENT_GO_SET_KEY = "environment:thirdparty:goagent:lock_upgrade"
        private const val LOCK_UPGRADE_AGENT_JDK_SET_KEY = "environment:thirdparty:jdk:lock_upgrade"
        private const val LOCK_UPGRADE_DOCKER_INIT_FILE_SET_KEY =
            "environment:thirdparty:docker_init_file:lock_upgrade"

        private const val FORCE_UPGRADE_AGENT_SET_KEY = "environment:thirdparty:force_upgrade"

        private const val FORCE_UPGRADE_AGENT_WORKER_SET_KEY = "environment:thirdparty:worker:force_upgrade"
        private const val FORCE_UPGRADE_AGENT_GO_SET_KEY = "environment:thirdparty:goagent:force_upgrade"
        private const val FORCE_UPGRADE_AGENT_JDK_SET_KEY = "environment:thirdparty:jdk:force_upgrade"
        private const val FORCE_UPGRADE_AGENT_DOCKER_INIT_FILE_SET_KEY =
            "environment:thirdparty:docker_init_file:force_upgrade"

        private val logger = LoggerFactory.getLogger(AgentScope::class.java)

        private val success = Result(true)
    }

    enum class UpgradeKey {
        LOCK_UPGRADE, FORCE_UPGRADE, CAN_UPGRADE
    }
}
