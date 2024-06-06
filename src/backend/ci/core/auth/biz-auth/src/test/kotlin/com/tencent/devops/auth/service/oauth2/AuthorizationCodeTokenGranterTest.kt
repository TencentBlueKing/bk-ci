package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.service.oauth2.grant.AuthorizationCodeTokenGranter
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.model.auth.tables.records.TAuthOauth2AccessTokenRecord
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@Disabled
// todo 到时候合并最新代码可消除
class AuthorizationCodeTokenGranterTest : BkCiAbstractTest() {

    private val codeService = mockk<Oauth2CodeService>()

    private val accessTokenService = mockk<Oauth2AccessTokenService>()

    private val refreshTokenService = mockk<Oauth2RefreshTokenService>()

    private val self: AuthorizationCodeTokenGranter = spyk(
        AuthorizationCodeTokenGranter(
            codeService = codeService,
            accessTokenService = accessTokenService,
            refreshTokenService = refreshTokenService
        ),
        recordPrivateCalls = true
    )
    private val clientDetails = ClientDetailsInfo(
        clientId = "testClientId",
        clientSecret = "testClientSecret",
        clientName = "testClientName",
        authorizedGrantTypes = "testGrantTypes",
        redirectUri = "testRedirectUri",
        scope = "testScope",
        accessTokenValidity = 3600,
        refreshTokenValidity = 3600,
        icon = "icon"
    )

    @Test
    fun `generateRefreshToken should return existing refreshToken when accessToken is valid`() {
        val accessTokenInfo = TAuthOauth2AccessTokenRecord(
            "testAccessToken",
            "testClientId",
            "testUserName",
            "",
            "testGrantType",
            System.currentTimeMillis() / 1000 + 1000,
            "testRefreshToken",
            1,
            LocalDateTime.now()
        )

        val refreshToken = self.invokePrivate<String>(
            "generateRefreshToken",
            clientDetails.clientId,
            clientDetails,
            accessTokenInfo
        )

        assertEquals(accessTokenInfo.refreshToken, refreshToken)
    }

    @Test
    fun `generateRefreshToken should return new refreshToken when accessToken is expired`() {
        val expiredAccessTokenInfo = TAuthOauth2AccessTokenRecord(
            "testAccessToken",
            "testClientId",
            "testUserName",
            "",
            "testGrantType",
            System.currentTimeMillis() / 1000 - 1000,
            "testRefreshToken",
            1,
            LocalDateTime.now()
        )
        every { refreshTokenService.create(any(), any(), any()) } returns Unit
        every { refreshTokenService.delete(any()) } returns Unit

        val refreshToken = self.invokePrivate<String>(
            "generateRefreshToken",
            clientDetails.clientId,
            clientDetails,
            expiredAccessTokenInfo
        )
        assertNotEquals(expiredAccessTokenInfo.refreshToken, refreshToken)
    }
}
