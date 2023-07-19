package com.tencent.devops.auth.service.oauth2.grant

import org.slf4j.LoggerFactory

abstract class AbstractTokenGranter(private val grantType: String) : TokenGranter {
    override fun grant(grantType: String): String? {
        if (this.grantType != grantType) {
            return null
        }
        //1.校验用户的授权类型是否合法
        logger.info("AbstractTokenGranter grant")
        return getAccessToken(grantType)
    }

    open fun getAccessToken(grantType: String): String {
        logger.info("Default getAccessToken")
        // 从入参中获取accessToken.
        // 1、校验access_token是否为空
        // 2.1 若为空，创建新的access_token并存储，返回access_token，过期时间，
        // 3.1 若不为空，校验access_token是否过期
        // 3.2 若过期，清除access_token记录，创建新的access_token并存储，返回access_token，过期时间。
        // 3.3 若未过期，返回access_token，过期时间
        return "Default"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractTokenGranter::class.java)
    }
}
