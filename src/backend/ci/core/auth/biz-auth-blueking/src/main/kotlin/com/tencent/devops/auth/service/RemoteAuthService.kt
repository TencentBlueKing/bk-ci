package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.devops.auth.utils.StringUtils
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RemoteAuthService @Autowired constructor(
    val redisOperation: RedisOperation,
    val tokenServiceImpl: TokenServiceImpl
) {

    @Value("\${auth.iamCallBackUser}")
    val iamClientName = ""

    fun checkToken(token: String): Boolean {
        val pair = StringUtils.decodeAuth(token)
        if (pair.first != iamClientName) {
            logger.warn("iam tokenCheck: userName error ${pair.first}")
            return false
        }
        val redisToken = redisOperation.get(TOKEN_REDIS_KEY)
        if (redisToken.isNullOrEmpty()) {
            logger.info("iam tokenCheck: redis is empty")
            val remoteToken = getRemoteToken()
            return if (remoteToken == pair.second) {
                redisOperation.set(TOKEN_REDIS_KEY, remoteToken)
                true
            } else {
                false
            }
        }
        logger.info("iam tokenCheck: redis data $redisToken")
        if (pair.second == redisToken) {
            return true
        }

        logger.info("iam tokenCheck: redis notEqual input, redis[$redisToken], input[${pair.second}]")
        // 最终验证权在auth服务端
        val remoteToken = getRemoteToken()
        if (pair.second == remoteToken) {
            return true
        }
        logger.info("iam tokenCheck fail]")
        return false
    }

    private fun getRemoteToken(): String {
        val token = tokenServiceImpl.token
        logger.info("get iam token: $token")
        return token
    }

    companion object {
        const val TOKEN_REDIS_KEY = "_BK:AUTH:V3:TOKEN_"
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}