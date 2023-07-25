package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthOauth2AccessTokenDao
import com.tencent.devops.auth.pojo.Oauth2AccessTokenInfo
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.ClientService
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

abstract class AbstractTokenGranter(
    private val grantType: String,
    private val clientService: ClientService,
    private val authOauth2AccessTokenDao: AuthOauth2AccessTokenDao,
    private val dslContext: DSLContext
) : TokenGranter {
    override fun grant(
        grantType: String,
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo? {
        if (this.grantType != grantType) {
            return null
        }
        // 1、校验client_id和client_secret是否正确
        val clientId = oauth2AccessTokenDTO.clientId
        val clientDetail = clientService.getClientDetail(oauth2AccessTokenDTO.clientId)
        if (oauth2AccessTokenDTO.clientSecret != clientDetail.clientSecret) {
            logger.warn("The client($clientId) secret is invalid")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_CLIENT_SECRET,
                params = arrayOf(clientId),
                defaultMessage = "The client($clientId) secret is invalid"
            )
        }
        // 2、校验用户的授权类型是否合法
        logger.info("AbstractTokenGranter grant")
        if (grantType != clientDetail.authorizedGrantTypes) {
            logger.warn("The client($clientId) grant type is invalid")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_AUTHORIZATION_TYPE,
                params = arrayOf(clientId),
                defaultMessage = "The client($clientId) grant type is invalid"
            )
        }
        return getAccessToken(oauth2AccessTokenDTO)
    }

    fun handleAccessToken(
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO,
        oauth2AccessTokenInfo: Oauth2AccessTokenInfo
    ): Oauth2AccessTokenVo {
        // 从入参中获取accessToken.
        // 1、校验access_token是否为空
        val accessToken = oauth2AccessTokenInfo.accessToken
        val refreshToken = oauth2AccessTokenInfo.refreshToken
        val clientId = oauth2AccessTokenDTO.clientId
        // 2.1 若为空，创建新的access_token并存储，返回access_token，过期时间，
        return if (accessToken == null) {
            authOauth2AccessTokenDao.create(
                dslContext = dslContext,
                clientId = clientId,
                userName = oauth2AccessTokenDTO.userName,
                // todo
                accessToken = "accessToken",
                refreshToken = "",
                expiredTime = 1000
            )
            Oauth2AccessTokenVo("accessToken", 1000, refreshToken)
        } else {
            // 3.1 若不为空，校验access_token是否过期
            if (oauth2AccessTokenInfo.expiredTime!!.toLong() > System.currentTimeMillis()) {
                // 3.2 若未过期，返回access_token，过期时间
                Oauth2AccessTokenVo(accessToken, 1000, refreshToken)
            } else {
                // 3.3 若过期，清除access_token记录，创建新的access_token并存储，返回access_token，过期时间。
                authOauth2AccessTokenDao.delete(
                    dslContext = dslContext,
                    accessToken = accessToken
                )
                authOauth2AccessTokenDao.create(
                    dslContext = dslContext,
                    clientId = clientId,
                    userName = oauth2AccessTokenDTO.userName,
                    // todo
                    accessToken = "accessToken",
                    refreshToken = "",
                    expiredTime = 1000
                )
                Oauth2AccessTokenVo("accessToken", 1000, refreshToken)
            }
        }
    }

    abstract fun getAccessToken(oauth2AccessTokenDTO: Oauth2AccessTokenDTO): Oauth2AccessTokenVo

    private fun createAccessToken(

    ) {
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractTokenGranter::class.java)
    }
}
