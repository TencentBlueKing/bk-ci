package com.tencent.devops.remotedev.service.redis

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.WhiteListDao
import com.tencent.devops.remotedev.pojo.WhiteListType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class RedisCacheService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    @Qualifier("redisStringHashOperation")
    private val redisHashOperation: RedisOperation,
    private val whiteListDao: WhiteListDao,
    private val expertSupportDao: ExpertSupportDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RedisCacheService::class.java)
    }

    private val redisCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String?> { key -> redisOperation.get(key) }

    private val redisCacheSet = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Set<String>?> { key -> redisOperation.getSetMembers(key) }

    private val redisCacheHash = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Map<String, String>?> { key -> redisHashOperation.hentries(key) }

    private val apiWhiteListCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Boolean> { key ->
            whiteListDao.get(dslContext, key, WhiteListType.API) != null
        }

    private val windowsGpuWhiteListCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Int> { key ->
            whiteListDao.get(dslContext, key, WhiteListType.WINDOWS_GPU)?.windowsGpuLimit ?: 0
        }

    private val expertSupportCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<ExpertSupportConfigType, List<String>> { type ->
            expertSupportDao.fetchExpertSupportConfig(dslContext, type).map { it.content }
        }

    fun get(key: String) = redisCache.get(key)

    fun getSetMembers(key: String) = redisCacheSet.get(key)

    fun hentries(key: String) = redisCacheHash.get(key)

    fun checkApiWhiteList(name: String) = apiWhiteListCache.get(name) ?: false

    fun checkWindowsGpuLimit(name: String) = windowsGpuWhiteListCache.get(name) ?: 0

    fun checkExpertSupportUser(userId: String) =
        expertSupportCache.get(ExpertSupportConfigType.SUPPORTER)?.contains(userId) ?: false
}
