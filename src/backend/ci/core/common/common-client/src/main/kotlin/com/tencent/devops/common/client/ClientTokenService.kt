package com.tencent.devops.common.client

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import org.springframework.beans.factory.annotation.Value

class ClientTokenService(
    val redisOperation: RedisOperation,
    val bkTag: BkTag
) {
    @Value("\${auth.token:#{null}}")
    private val systemToken: String = ""

    fun getSystemToken(): String {
        return systemToken
    }

    fun checkToken(token: String): Boolean {
        return systemToken == token
    }

    companion object {
        const val DEFAULT_APP = "ci"
    }
}
