package com.tencent.devops.auth.service

import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteAuthService @Autowired constructor(
    val redisOperation: RedisOperation
) {

    fun checkToken(token: String): Boolean {
        val redisToken = redisOperation.get(TOKEN_REDIS_KEY)
        if (redisToken.isNullOrEmpty()) {
            val remoteToken = getRemoteToken()
            return if (remoteToken == token) {
                redisOperation.set(TOKEN_REDIS_KEY, remoteToken)
                true
            } else {
                false
            }
        }
        if (token == redisToken) {
            return true
        }
        return false
    }

    private fun getRemoteToken(): String {
        return ""
    }

    companion object {
        const val TOKEN_REDIS_KEY = "_BK:AUTH:V3:TOKEN_"
    }
}