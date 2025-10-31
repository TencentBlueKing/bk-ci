package com.tencent.devops.remotedev.service.redis

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.dao.ConfigDao
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.WhiteListDao
import com.tencent.devops.remotedev.pojo.WhiteListType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ConfigCacheService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val whiteListDao: WhiteListDao,
    private val expertSupportDao: ExpertSupportDao,
    private val configDao: ConfigDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ConfigCacheService::class.java)
    }

    fun opFetchAllConfig(): Map<String, String> {
        logger.info("fetch all config")
        return configDao.fetchAll(dslContext).associateBy({ it.key }, { it.value })
    }

    fun opInsertOrUpdateConfig(key: String, value: String): Boolean {
        logger.info("insert or update config, key: $key, value: $value")
        return configDao.insertOrUpdateConfig(dslContext, key, value)
    }

    fun opDeleteConfig(key: String): Boolean {
        logger.info("delete config, key: $key")
        return configDao.deleteConfig(dslContext, key)
    }

    private val userNameCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String> { key ->
            runCatching {
                client.get(ServiceTxUserResource::class).get(key).data?.name
            }.getOrNull() ?: ""
        }

    private val redisCache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String?> { key -> configDao.fetchConfig(dslContext, key) ?: redisOperation.get(key) }

    private val redisCacheSet = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Set<String>?> { key -> redisOperation.getSetMembers(key) }

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

    fun get(key: String): String? = redisCache.get(key)

    fun getSetMembers(key: String) = redisCacheSet.get(key)

    fun checkApiWhiteList(name: String) = apiWhiteListCache.get(name) ?: false

    fun checkWindowsGpuLimit(name: String) = windowsGpuWhiteListCache.get(name) ?: 0

    fun checkExpertSupportUser(userId: String) =
        expertSupportCache.get(ExpertSupportConfigType.SUPPORTER)?.contains(userId) ?: false

    fun getUserName(key: String): String = userNameCache.get(key)
}
