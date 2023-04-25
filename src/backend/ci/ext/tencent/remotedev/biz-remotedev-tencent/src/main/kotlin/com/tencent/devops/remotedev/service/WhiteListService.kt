package com.tencent.devops.remotedev.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WhiteListService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val cacheService: RedisCacheService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }

    // 添加客户端白名单用户。目前对接redis配置，后续需要对接权限系统。
    fun addWhiteListUser(userId: String, whiteListUser: String): Boolean {
        logger.info("userId($userId) wants to add whiteListUser($whiteListUser)")
        // whiteListUser支持多个用;分隔，需要解析。
        if (whiteListUser.isEmpty()) return false
        val whiteListUserArray = whiteListUser.split(";")
        for (user in whiteListUserArray) {
            if (cacheService.getSetMembers(RedisKeys.REDIS_WHITE_LIST_KEY)?.contains(user) != true) {
                logger.info("whiteListUser($user) not in the whiteList")
                redisOperation.addSetValue(RedisKeys.REDIS_WHITE_LIST_KEY, user, false)
            }
        }
        return true
    }
}
