package com.tencent.devops.common.client

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ClientTokenService @Autowired constructor(
    val redisOperation: RedisOperation,
    val bkTag: BkTag
) {
    @Value("\${auth.token:#{null}}")
    private val systemToken: String? = ""

    fun getSystemToken(appCode: String?): String? {
        return redisOperation.get(getTokenRedisKey(appCode ?: DEFAULT_APP))
    }

    fun setSystemToken(appCode: String?) {
        redisOperation.set(getTokenRedisKey(appCode ?: DEFAULT_APP), systemToken!!)
    }

    fun checkToken(appCode: String?, token: String): Boolean {
        val systemToken = getSystemToken(appCode)
        return systemToken == token
    }

    private fun getTokenRedisKey(appCode: String): String {
        return "BK:AUTH:TOKEN:${bkTag.getTag()}:$appCode:"
    }

    companion object {
        const val DEFAULT_APP = "ci"
    }
}
