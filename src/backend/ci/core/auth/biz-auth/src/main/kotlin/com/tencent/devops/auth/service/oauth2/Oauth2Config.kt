package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.service.oauth2.grant.AuthorizationCodeTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.ClientCredentialsTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.CompositeTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.RefreshTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Oauth2Config {
    @Bean
    fun tokenService(): TokenService {
        return TokenService(compositeTokenGranter())
    }

    @Bean
    fun compositeTokenGranter(): TokenGranter {
        return CompositeTokenGranter(getDefaultTokenGranters())
    }

    private fun getDefaultTokenGranters(): List<TokenGranter> {
        val tokenGranters = ArrayList<TokenGranter>()
        val refreshTokenGranter = RefreshTokenGranter()
        val clientCredentialsTokenGranter = ClientCredentialsTokenGranter()
        val authorizationCodeTokenGranter = AuthorizationCodeTokenGranter()
        tokenGranters.add(refreshTokenGranter)
        tokenGranters.add(clientCredentialsTokenGranter)
        tokenGranters.add(authorizationCodeTokenGranter)
        return tokenGranters
    }
}
