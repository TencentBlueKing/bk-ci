package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
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

    fun addGPUWhiteListUser(userId: String, whiteListUser: String): Boolean {
        logger.info("userId($userId) wants to add GPU whiteListUser($whiteListUser)")
        // whiteListUser支持多个用;分隔，需要解析。
        whiteListUser.apply {
            val whiteListUserArray = this.split(";")
            for (user in whiteListUserArray) {
                cacheService.hentries(RedisKeys.REDIS_WHITE_LIST_GPU_KEY)?.get(user)?: run {
                    logger.info("whiteListUser($user) not in the GPU whiteList")
                    redisOperation.hmset(RedisKeys.REDIS_WHITE_LIST_GPU_KEY, mapOf(user.toString() to "1"))
                }
            }
        }

        return true
    }

    /* 有关数量的限制:
        如果value大于指定key中id规定的数量，则抛出异常
        如果没有白名单，则抛出异常
        如果白名单中没有对应id，则抛出异常
        */
    fun numberLimit(key: String, id: String, value: Long) {
        val limit = cacheService.hentries(key)?.get(id)?.toLong()
        logger.info("numberLimit|$key|$id|$value|$limit")
        if (limit != null && value < limit) {
            // 没有达到限制，直接return
            return
        }
        throw ErrorCodeException(
            errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
            params = arrayOf("User($id) not in the whiteList or exceeding the limit")
        )
    }
}
