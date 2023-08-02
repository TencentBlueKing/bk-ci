package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.service.oauth2.grant.AuthorizationCodeTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.ClientCredentialsTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.CompositeTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.RefreshTokenGranter
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Suppress("LongParameterList")
class Oauth2Config constructor(
    private val oauth2ClientService: Oauth2ClientService,
    private val codeService: Oauth2CodeService,
    private val clientCredentialsTokenGranter: ClientCredentialsTokenGranter,
    private val authorizationCodeTokenGranter: AuthorizationCodeTokenGranter,
    private val refreshTokenGranter: RefreshTokenGranter,
    private val dslContext: DSLContext,
    private val authActionDao: AuthActionDao
) {
    @Bean
    fun oauth2EndpointService(): Oauth2EndpointService {
        return Oauth2EndpointService(
            tokenGranter = compositeTokenGranter(),
            clientService = oauth2ClientService,
            codeService = codeService,
            dslContext = dslContext,
            authActionDao = authActionDao
        )
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
