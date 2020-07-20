package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.devops.auth.utils.StringUtils
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteAuthService @Autowired constructor(
    val redisOperation: RedisOperation,
    val tokenServiceImpl: TokenServiceImpl
) {

    fun checkToken(token: String): Boolean {
        val pair = StringUtils.decodeAuth(token)
        // TODO: 配置化
        if(pair.first != "bk_iam") {
            return false
        }
        val redisToken = redisOperation.get(TOKEN_REDIS_KEY)
        if (redisToken.isNullOrEmpty()) {
            val remoteToken = getRemoteToken()
            return if (remoteToken == pair.second) {
                redisOperation.set(TOKEN_REDIS_KEY, remoteToken)
                true
            } else {
                false
            }
        }
        if (pair.second == redisToken) {
            return true
        }

        // 最终验证权在auth服务端
        val remoteToken = getRemoteToken()
        if(pair.second == remoteToken) {
            return true
        }
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