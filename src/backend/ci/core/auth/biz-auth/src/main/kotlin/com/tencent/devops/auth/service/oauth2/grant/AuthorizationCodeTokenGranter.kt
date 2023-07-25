package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.constant.AuthMessageCode.INVALID_AUTHORIZATION_CODE
import com.tencent.devops.auth.dao.AuthOauth2AccessTokenDao
import com.tencent.devops.auth.dao.AuthOauth2CodeDao
import com.tencent.devops.auth.dao.AuthOauth2RefreshTokenDao
import com.tencent.devops.auth.pojo.Oauth2AccessTokenInfo
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.ClientService
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthorizationCodeTokenGranter constructor(
    private val clientService: ClientService,
    private val authOauth2RefreshTokenDao: AuthOauth2RefreshTokenDao,
    private val authOauth2AccessTokenDao: AuthOauth2AccessTokenDao,
    private val authOauth2CodeDao: AuthOauth2CodeDao,
    private val dslContext: DSLContext
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    clientService = clientService,
    authOauth2AccessTokenDao = authOauth2AccessTokenDao,
    dslContext = dslContext
) {
    companion object {
        private const val GRANT_TYPE = "authorization_code"
        private val logger = LoggerFactory.getLogger(AuthorizationCodeTokenGranter::class.java)
    }

    override fun getAccessToken(
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo {
        logger.info("authorization_code getAccessToken")
        val clientId = oauth2AccessTokenDTO.clientId
        val code = oauth2AccessTokenDTO.code
            ?: throw ErrorCodeException(
                errorCode = INVALID_AUTHORIZATION_CODE,
                defaultMessage = "The authorization code must be provided"
            )
        // 1.获取授权码，判断授权是否为空
        val codeDetails = authOauth2CodeDao.get(
            dslContext = dslContext,
            code = code
        ) ?: throw ErrorCodeException(
            errorCode = INVALID_AUTHORIZATION_CODE,
            defaultMessage = "The authorization code invalid"
        )
        // 2.判断授权码和客户端id是否对应的上
        if (codeDetails.clientId == clientId) {
            throw ErrorCodeException(
                errorCode = INVALID_AUTHORIZATION_CODE,
                defaultMessage = "The authorization code does not belong to the client($clientId)"
            )
        }
        // 3.判断授权码是否过期
        if (codeDetails.expiredTime < System.currentTimeMillis()) {
            throw ErrorCodeException(
                errorCode = INVALID_AUTHORIZATION_CODE,
                defaultMessage = "The authorization code expired"
            )
        }
        // 4.若授权码没有问题，则直接消费授权码，授权码单次有效，直接数据库删除该授权码
        authOauth2CodeDao.delete(
            dslContext = dslContext,
            code = code
        )
        // 5、根据appcode+username获取accessToken
        val accessTokenRecord = authOauth2AccessTokenDao.get(
            dslContext = dslContext,
            clientId = clientId,
            userName = oauth2AccessTokenDTO.userName
        )
        val refreshToken = if (accessTokenRecord != null) {
            val refreshTokenRecord = authOauth2RefreshTokenDao.get(
                dslContext = dslContext,
                refreshToken = accessTokenRecord.refreshToken,
            )
            if (refreshTokenRecord!!.expiredTime > System.currentTimeMillis()) {
                refreshTokenRecord.refreshToken
            } else {
                authOauth2RefreshTokenDao.delete(
                    dslContext = dslContext,
                    refreshToken = accessTokenRecord.refreshToken
                )
                authOauth2RefreshTokenDao.create(
                    dslContext = dslContext,
                    refreshToken = "refreshToken",
                    clientId = clientId,
                    expiredTime = 1000
                )
                "refreshToken"
            }
        } else {
            "refreshToken"
        }

        return super.handleAccessToken(
            oauth2AccessTokenDTO,
            Oauth2AccessTokenInfo(
                accessToken = accessTokenRecord?.accessToken,
                refreshToken = refreshToken,
                expiredTime = accessTokenRecord?.expiredTime
            )
        )
    }
}
