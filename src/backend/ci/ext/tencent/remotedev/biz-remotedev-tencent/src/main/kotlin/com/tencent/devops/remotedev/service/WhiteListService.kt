package com.tencent.devops.remotedev.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.service.redis.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class WhiteListService @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    private val redisCache = Caffeine.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String?> { key -> redisOperation.get(key) }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }

    // 添加客户端白名单用户。目前对接redis配置，后续需要对接权限系统。
    fun addWhiteListUser(userId: String, whiteListUser: String): Boolean {
        logger.info("userId($userId) wants to add whiteListUser($whiteListUser)")
        if (redisCache.get(RedisKeys.REDIS_WHITE_LIST_KEY)?.contains(whiteListUser) != true) {
            logger.info("whiteListUser($whiteListUser) not in the whiteList")
            redisOperation.setIfAbsent(RedisKeys.REDIS_WHITE_LIST_KEY, whiteListUser, null, false, false)
            return true
        }
        return true
    }
}
