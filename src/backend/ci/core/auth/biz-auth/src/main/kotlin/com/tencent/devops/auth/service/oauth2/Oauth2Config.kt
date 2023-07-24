package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.service.oauth2.grant.AuthorizationCodeTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.ClientCredentialsTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.CompositeTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.RefreshTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Oauth2Config constructor(
    private val clientCredentialsTokenGranter: ClientCredentialsTokenGranter,
    private val authorizationCodeTokenGranter: AuthorizationCodeTokenGranter,
    private val refreshTokenGranter: RefreshTokenGranter,
) {
    @Bean
    fun oauth2EndpointService(): Oauth2EndpointService {
        return Oauth2EndpointService(compositeTokenGranter())
    }

    @Bean
    fun compositeTokenGranter(): TokenGranter {
        return CompositeTokenGranter(getDefaultTokenGranters())
    }

    private fun getDefaultTokenGranters(): List<TokenGranter> {
        val tokenGranters = ArrayList<TokenGranter>()
        tokenGranters.add(clientCredentialsTokenGranter)
        tokenGranters.add(authorizationCodeTokenGranter)
        tokenGranters.add(refreshTokenGranter)
        return tokenGranters
    }
}
