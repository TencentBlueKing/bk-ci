package com.tencent.devops.remotedev.service.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.pojo.WorkSpaceCacheInfo
import com.tencent.devops.remotedev.service.redis.RedisKeys.WORKSPACE_CACHE_KEY_PREFIX
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisCacheService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RedisCacheService::class.java)
        const val CACHE_EXPIRE_TIME = 1800L
    }

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

    fun saveWorkspaceDetail(
        workspaceName: String,
        cache: WorkSpaceCacheInfo
    ) {
        logger.info("save workspace detail from redis|$workspaceName|$cache")
        redisOperation.set(
            key = "$WORKSPACE_CACHE_KEY_PREFIX:$workspaceName",
            value = JsonUtil.toJson(cache),
            expiredInSecond = CACHE_EXPIRE_TIME
        )
    }

    fun deleteWorkspaceDetail(workspaceName: String) {
        logger.info("delete workspace detail from redis|$workspaceName")
        redisOperation.delete("$WORKSPACE_CACHE_KEY_PREFIX$workspaceName")
    }

    fun getWorkspaceDetail(workspaceName: String): WorkSpaceCacheInfo? {
        return try {
            val result = redisOperation.get(
                "$WORKSPACE_CACHE_KEY_PREFIX:$workspaceName"
            )
            if (result != null) {
                objectMapper.readValue<WorkSpaceCacheInfo>(result)
            } else {
                null
            }
        } catch (ignore: Exception) {
            logger.warn(
                "get workspace detail from redis error|$workspaceName",
                ignore
            )
            null
        }
    }
}
