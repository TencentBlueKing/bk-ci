package com.tencent.devops.remotedev.service.redis

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisCacheService @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    private val redisCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String?> { key -> redisOperation.get(key) }

    private val redisCacheSet = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Set<String>?> { key -> redisOperation.getSetMembers(key) }

    fun get(key: String) = redisCache.get(key)

    fun getSetMembers(key: String) = redisCacheSet.get(key)
}
