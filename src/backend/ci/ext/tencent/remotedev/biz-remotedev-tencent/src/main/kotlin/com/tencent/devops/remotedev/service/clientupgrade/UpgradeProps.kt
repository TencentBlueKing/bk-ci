package com.tencent.devops.remotedev.service.clientupgrade

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.redis.RedisOperation
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
        loadCache(PARALLEL_UPGRADE_COUNT, isDistinguishCluster = false).toIntOrNull() ?: DEFAULT_PARALLEL_UPGRADE_COUNT

    fun setCanUpgradeClients(newIds: Set<String>) {
        logger.debug("setCanUpgradeClients, ids: $newIds")
        var change = false
        val existingClientsMacAddress = loadIdCache(CAN_UPGRADE_CLIENT_SET_KEY, isDistinguishCluster = true)
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
            invalidateIdCache(CAN_UPGRADE_CLIENT_SET_KEY)
        }
    }

    fun checkCanUpgrade(macAddress: String): Boolean {
        return loadIdCache(CAN_UPGRADE_CLIENT_SET_KEY, isDistinguishCluster = true).contains(macAddress)
    }

    fun getClientVersion() = loadCache(CURRENT_CLIENT_VERSION, isDistinguishCluster = true)

    fun getStartVersion() = loadCache(CURRENT_START_VERSION, isDistinguishCluster = true)

    private val idCache: Cache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build()

    private fun loadIdCache(cacheKey: String, isDistinguishCluster: Boolean = false): Set<String> {
        return idCache.get(cacheKey) {
            redisOperation.getSetMembers(
                cacheKey, isDistinguishCluster
            )?.filter { it.isNotBlank() }?.map { it }?.toSet() ?: setOf()
        } ?: setOf()
    }

    private fun invalidateIdCache(cacheKey: String) {
        idCache.invalidate(cacheKey)
    }

    private val distinguishCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { key -> redisOperation.get(key, isDistinguishCluster = true) ?: "" }

    private val singleCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { key -> redisOperation.get(key, isDistinguishCluster = false) ?: "" }

    private fun loadCache(redisKey: String, isDistinguishCluster: Boolean = true): String =
        (if (isDistinguishCluster) distinguishCache.get(redisKey) else singleCache.get(redisKey)) ?: ""

    companion object {
        private const val CACHE_EXPIRE_MIN = 1L
        private const val CACHE_SIZE = 100L

        private const val PARALLEL_UPGRADE_COUNT = "remotedev:clientupgrade:parallel.upgrade.count"
        private const val DEFAULT_PARALLEL_UPGRADE_COUNT = 50

        private const val CAN_UPGRADE_CLIENT_SET_KEY = "remotedev:clientupgrade:can_upgrade"

        private const val CURRENT_CLIENT_VERSION = "remotedev:clientupgrade:client.verison"
        private const val CURRENT_START_VERSION = "remotedev:clientupgrade:start.verison"

        private val logger = LoggerFactory.getLogger(UpgradeProps::class.java)
    }
}