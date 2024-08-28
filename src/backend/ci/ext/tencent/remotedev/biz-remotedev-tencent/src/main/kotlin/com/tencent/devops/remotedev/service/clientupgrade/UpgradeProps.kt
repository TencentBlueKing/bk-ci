package com.tencent.devops.remotedev.service.clientupgrade

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.pojo.ClientUpgradeComp
import com.tencent.devops.remotedev.pojo.ClientUpgradeKind
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeOpType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 一些升级参数，单独抽出方便清晰区分
 */
@Service
class UpgradeProps @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    fun getMaxParallelUpgradeCount(): Int =
        loadCache(PARALLEL_UPGRADE_COUNT, isDistinguishCluster = true).toIntOrNull() ?: DEFAULT_PARALLEL_UPGRADE_COUNT

    fun setMaxParallelUpgradeCount(count: Int) {
        redisOperation.set(
            key = PARALLEL_UPGRADE_COUNT,
            value = count.toString(),
            expiredInSecond = null,
            expired = false,
            isDistinguishCluster = true
        )
        invalidateCache(PARALLEL_UPGRADE_COUNT, true)
    }

    fun setCanUpgradeClients(newIds: Set<String>) {
        logger.debug("setCanUpgradeClients, ids: $newIds")
        var change = false
        val existingClientsMacAddress = loadSetCache(CAN_UPGRADE_CLIENT_SET_KEY, isDistinguishCluster = true)
        val toAddClientsMacAddress = newIds.filterNot { existingClientsMacAddress.contains(it) }
        if (toAddClientsMacAddress.isNotEmpty()) {
            toAddClientsMacAddress.forEach {
                redisOperation.addSetValue(CAN_UPGRADE_CLIENT_SET_KEY, it, true)
            }
            change = true
        }
        val toDeleteClientsMacAddress = existingClientsMacAddress.filterNot { newIds.contains(it) }
        if (toDeleteClientsMacAddress.isNotEmpty()) {
            toDeleteClientsMacAddress.forEach {
                redisOperation.removeSetMember(CAN_UPGRADE_CLIENT_SET_KEY, it, true)
            }
            change = true
        }

        logger.debug(
            "$change|toAddIds=$toAddClientsMacAddress" +
                    "|existIds=$existingClientsMacAddress|toDeleteIds=$toDeleteClientsMacAddress"
        )

        if (change) {
            invalidateSetCache(CAN_UPGRADE_CLIENT_SET_KEY)
        }
    }

    fun checkCanUpgrade(macAddress: String): Boolean {
        return loadSetCache(CAN_UPGRADE_CLIENT_SET_KEY, isDistinguishCluster = true).contains(macAddress)
    }

    fun getCurrentVersion(comp: ClientUpgradeComp, os: OS) =
        loadCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.CURRENT_VERSION), isDistinguishCluster = true)

    fun setCurrentVersion(comp: ClientUpgradeComp, os: OS, version: String) {
        setUpgradeStringCache(comp, os, ClientUpgradeKind.CURRENT_VERSION, version)
    }

    fun getMaxNumb(comp: ClientUpgradeComp, os: OS) =
        loadCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.MAX_NUMB), isDistinguishCluster = true).toIntOrNull()

    fun setMaxNumb(comp: ClientUpgradeComp, os: OS, numb: Int) {
        setUpgradeStringCache(comp, os, ClientUpgradeKind.MAX_NUMB, numb.toString())
    }

    private fun setUpgradeStringCache(comp: ClientUpgradeComp, os: OS, kind: ClientUpgradeKind, version: String) {
        val key = genUpgradeRedisKey(comp, os, kind)
        redisOperation.set(
            key = key,
            value = version,
            expiredInSecond = null,
            expired = false,
            isDistinguishCluster = true
        )
        invalidateCache(key, true)
    }

    fun getUserVersion(comp: ClientUpgradeComp, os: OS) =
        loadHashCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.CURRENT_USER_VERSION), true)

    fun setUserVersion(comp: ClientUpgradeComp, os: OS, version: Map<String, String>, opType: ClientUpgradeOpType) =
        opHashCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.CURRENT_USER_VERSION), version, opType, true)

    fun getWorkspaceNameVersion(comp: ClientUpgradeComp, os: OS) =
        loadHashCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.CURRENT_WORKSPACE_NAME_VERSION), true)

    fun setWorkspaceNameVersion(comp: ClientUpgradeComp, os: OS, version: Map<String, String>, opType: ClientUpgradeOpType) =
        opHashCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.CURRENT_WORKSPACE_NAME_VERSION), version, opType, true)

    fun getProjectVersion(comp: ClientUpgradeComp, os: OS) =
        loadHashCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.CURRENT_PROJECT_VERSION), true)

    fun setProjectVersion(comp: ClientUpgradeComp, os: OS, version: Map<String, String>, opType: ClientUpgradeOpType) =
        opHashCache(genUpgradeRedisKey(comp, os, ClientUpgradeKind.CURRENT_PROJECT_VERSION), version, opType, true)

    private fun opHashCache(
        redisKey: String,
        values: Map<String, String>,
        opType: ClientUpgradeOpType,
        isDistinguishCluster: Boolean
    ) {
        when (opType) {
            ClientUpgradeOpType.ADD -> {
                redisOperation.hmset(redisKey, values, isDistinguishCluster)
                invalidateHashCache(redisKey)
            }

            ClientUpgradeOpType.REMOVE -> {
                redisOperation.hdelete(redisKey, values.keys.toTypedArray(), isDistinguishCluster)
                invalidateHashCache(redisKey)
            }

            ClientUpgradeOpType.REWRITE -> {
                redisOperation.delete(redisKey, isDistinguishCluster)
                redisOperation.hmset(redisKey, values, isDistinguishCluster)
                invalidateHashCache(redisKey)
            }
        }
    }

    private val distinguishCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { key -> redisOperation.get(key, isDistinguishCluster = true) ?: "" }

    private val singleCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { key -> redisOperation.get(key, isDistinguishCluster = false) ?: "" }

    private fun loadCache(redisKey: String, isDistinguishCluster: Boolean = true): String = if (isDistinguishCluster) {
        distinguishCache.get(redisKey)
    } else {
        singleCache.get(redisKey)
    } ?: ""

    private fun invalidateCache(redisKey: String, isDistinguishCluster: Boolean) {
        if (isDistinguishCluster) {
            distinguishCache.invalidate(redisKey)
        } else {
            singleCache.invalidate(redisKey)
        }
    }

    private val setCache: Cache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build()

    private fun loadSetCache(cacheKey: String, isDistinguishCluster: Boolean = false): Set<String> {
        return setCache.get(cacheKey) {
            redisOperation.getSetMembers(
                cacheKey, isDistinguishCluster
            )?.filter { it.isNotBlank() }?.map { it }?.toSet() ?: setOf()
        } ?: setOf()
    }

    private fun invalidateSetCache(cacheKey: String) {
        setCache.invalidate(cacheKey)
    }

    private val hashCache: Cache<String, Map<String, String>> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE).expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build()

    private fun loadHashCache(cacheKey: String, isDistinguishCluster: Boolean = true): Map<String, String> {
        return hashCache.get(cacheKey) {
            redisOperation.hentries(
                cacheKey, isDistinguishCluster
            )?.filter { it.key.isNotBlank() && it.value.isNotBlank() } ?: mapOf()
        } ?: mapOf()
    }

    private fun invalidateHashCache(cacheKey: String) {
        hashCache.invalidate(cacheKey)
    }

    companion object {
        private const val CACHE_EXPIRE_MIN = 1L
        private const val CACHE_SIZE = 100L

        private const val REMOTEDEV_UPGRADE_KEY_PREFIX = "remotedev:clientupgrade"

        private const val PARALLEL_UPGRADE_COUNT = "$REMOTEDEV_UPGRADE_KEY_PREFIX:parallel.upgrade.count"
        private const val DEFAULT_PARALLEL_UPGRADE_COUNT = 50

        private const val CAN_UPGRADE_CLIENT_SET_KEY = "$REMOTEDEV_UPGRADE_KEY_PREFIX:can_upgrade"

        private fun genUpgradeRedisKey(comp: ClientUpgradeComp, os: OS, kind: ClientUpgradeKind): String {
            return "$REMOTEDEV_UPGRADE_KEY_PREFIX:${comp.value}:${os.name.lowercase()}:${kind.value}"
        }

        private val logger = LoggerFactory.getLogger(UpgradeProps::class.java)
    }
}
