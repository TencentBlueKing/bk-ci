package com.tencent.devops.environment.service.thirdpartyagent.upgrade

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.pojo.AgentUpgradeType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Agent在项目范围内升级配置和操作
 * 检查优先升级的项目列表[checkInPriorityUpgradeProjectOrEmpty]
 * 检查禁止升级的项目列表[checkDenyUpgradeProject]
 */
@Component
class ProjectScope @Autowired constructor(private val redisOperation: RedisOperation) {

    private val success = Result(true)

    private fun getKeyType(upgradeKey: UpgradeKey, type: AgentUpgradeType?): String {
        if (type == AgentUpgradeType.WORKER) {
            return when (upgradeKey) {
                UpgradeKey.PRIORITY_PROJECT -> WORKER_PRIORITY_UPGRADE_PROJECT_SET // 优先升级项目
                UpgradeKey.DENY_PROJECT -> WORKER_DENY_UPGRADE_PROJECT_SET // 禁止升级项目
            }
        }
        // 默认全走Agent
        return when (upgradeKey) {
            UpgradeKey.PRIORITY_PROJECT -> AGENT_PRIORITY_UPGRADE_PROJECT_SET // 优先升级项目
            UpgradeKey.DENY_PROJECT -> AGENT_DENY_UPGRADE_PROJECT_SET // 禁止升级项目
        }
    }

    /**
     * 如果[projectId]在优先升级列表[AGENT_PRIORITY_UPGRADE_PROJECT_SET]中，返回true
     * 或者[AGENT_PRIORITY_UPGRADE_PROJECT_SET]为空，表示未设置任何优先升级项目，也返回true
     */
    fun checkInPriorityUpgradeProjectOrEmpty(projectId: String, type: AgentUpgradeType?): Boolean {
        val cache = fetchInPriorityUpgradeProject(type)
        return cache.isEmpty() || cache.contains(projectId)
    }

    // 获取升级列表的项目，方便早点升级
    fun fetchInPriorityUpgradeProject(type: AgentUpgradeType?): Set<String> {
        return loadCache(getKeyType(UpgradeKey.PRIORITY_PROJECT, type))
    }

    fun setUpgradeProjects(
        upgradeKey: UpgradeKey,
        projectIds: Set<String>,
        type: AgentUpgradeType?
    ): Result<Boolean> {
        val cacheKey = getKeyType(upgradeKey, type)
        logger.info("setUpgradeProjects_$upgradeKey| $projectIds")
        val failList = mutableSetOf<String>()
        projectIds.forEach { projectId ->
            if (!redisOperation.addSetValue(cacheKey, projectId, isDistinguishCluster = true)) {
                failList.add(projectId)
            }
        }

        if (failList.size < projectIds.size) {
            invalidateCache(cacheKey)
        }

        if (failList.isNotEmpty()) {
            return Result(data = false, message = "fail list: $failList")
        }

        return success
    }

    fun unsetUpgradeProjects(
        upgradeKey: UpgradeKey,
        projectIds: Set<String>,
        type: AgentUpgradeType?
    ): Result<Boolean> {
        val cacheKey = getKeyType(upgradeKey, type)
        logger.info("unsetUpgradeProjects_$upgradeKey| $projectIds")
        val failList = mutableSetOf<String>()
        projectIds.forEach { projectId ->
            if (!redisOperation.removeSetMember(cacheKey, projectId, isDistinguishCluster = true)) {
                failList.add(projectId)
            }
        }

        if (failList.size < projectIds.size) {
            invalidateCache(cacheKey)
        }

        if (failList.isNotEmpty()) {
            return Result(data = false, message = "fail list: $failList")
        }
        return success
    }

    fun getAllUpgradeProjects(upgradeKey: UpgradeKey, type: AgentUpgradeType?) = loadCache(getKeyType(upgradeKey, type))

    fun cleanAllUpgradeProjects(upgradeKey: UpgradeKey, type: AgentUpgradeType?): Boolean {
        val key = getKeyType(upgradeKey, type)
        val result = redisOperation.delete(key, isDistinguishCluster = true)
        invalidateCache(key)
        return result
    }

    fun checkDenyUpgradeProject(projectId: String, type: AgentUpgradeType?): Boolean {
        return when (type) {
            AgentUpgradeType.WORKER -> loadCache(WORKER_DENY_UPGRADE_PROJECT_SET).contains(projectId)
            else -> loadCache(AGENT_DENY_UPGRADE_PROJECT_SET).contains(projectId)
        }
    }

    private fun loadCache(key: String): Set<String> = projectCache.get(key) ?: setOf()

    private fun invalidateCache(cacheKey: String) {
        projectCache.invalidate(cacheKey)
    }

    private val projectCache: LoadingCache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { cacheKey ->
            redisOperation.getSetMembers(cacheKey, isDistinguishCluster = true)
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: setOf()
        }

    companion object {
        private const val CACHE_EXPIRE_MIN = 1L
        private const val CACHE_SIZE = 16L

        private const val AGENT_PRIORITY_UPGRADE_PROJECT_SET = "environment:thirdparty:agent.priority.upgrade.project"
        private const val AGENT_DENY_UPGRADE_PROJECT_SET = "environment:thirdparty:agent.deny.upgrade.project"
        private const val WORKER_PRIORITY_UPGRADE_PROJECT_SET = "environment:thirdparty:worker.priority.upgrade.project"
        private const val WORKER_DENY_UPGRADE_PROJECT_SET = "environment:thirdparty:worker.deny.upgrade.project"
        private val logger = LoggerFactory.getLogger(ProjectScope::class.java)
    }

    enum class UpgradeKey {
        PRIORITY_PROJECT, DENY_PROJECT
    }
}
