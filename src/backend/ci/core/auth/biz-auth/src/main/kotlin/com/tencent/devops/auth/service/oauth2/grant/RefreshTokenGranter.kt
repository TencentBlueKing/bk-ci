package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.Oauth2AccessTokenInfo
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RefreshTokenGranter : AbstractTokenGranter(GRANT_TYPE) {
    companion object {
        private const val GRANT_TYPE = "refresh_token"
        private val logger = LoggerFactory.getLogger(RefreshTokenGranter::class.java)
    }

    override fun getAccessToken(
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo {
        logger.info("refresh_token getAccessToken")
        //1.校验refresh_token是否为空
        //2.清除跟该refresh_token授权码相关的access_token
        //3.校验refresh_token是否过期
        //3.1 过期，清除refresh_token记录,则直接返回异常
        //3.2 未过期,走主流程,此时accessToken
        val accessToken = super.handleAccessToken(Oauth2AccessTokenInfo())
        return Oauth2AccessTokenVo("accessToken", 1000)
    }
}
