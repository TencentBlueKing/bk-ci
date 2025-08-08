package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.SpringContextUtil

object Oauth2TokenGranterFactory {
    fun <T : Oauth2AccessTokenRequest> getTokenGranter(accessTokenRequest: T): TokenGranter<T> {
        val tokenGranters = SpringContextUtil.getBeansWithClass(TokenGranter::class.java)
        val grantType = accessTokenRequest.grantType
        for (tokenGranter in tokenGranters) {
            if (grantType == tokenGranter.type()) {
                return (tokenGranter as TokenGranter<T>)
            }
        }
        throw ErrorCodeException(
            errorCode = AuthMessageCode.INVALID_AUTHORIZATION_TYPE,
            defaultMessage = "The client does not support $grantType type"
        )
    }
}
