package com.tencent.devops.process.engine.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 流水线引擎动态配置管理类
 * 用来放一些引擎中需要动态配置的参数方法
 */
@Service
class EngineConfigService @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    private val localCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { key -> redisOperation.get(key, isDistinguishCluster = false) ?: "" }

    fun getMutexMaxQueue() =
        localCache.get(ENGINE_CONFIG_MUTEX_MAX_QUEUE_KEY)?.ifBlank { null }?.toIntOrNull() ?: MUTEX_MAX_QUEUE_DEFAULT

    companion object {
        private const val ENGINE_CONFIG_MUTEX_MAX_QUEUE_KEY = "engine.config.mutex.maxQueue"
        const val MUTEX_MAX_QUEUE_DEFAULT = 50

        private const val CACHE_EXPIRE_MIN = 10L
        private const val CACHE_SIZE = 100L
    }
}