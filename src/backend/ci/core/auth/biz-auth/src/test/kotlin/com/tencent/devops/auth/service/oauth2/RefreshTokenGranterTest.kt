package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_REFRESH_TOKEN_EXPIRED
import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.service.oauth2.grant.RefreshTokenGranter
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.model.auth.tables.records.TAuthOauth2AccessTokenRecord
import com.tencent.devops.model.auth.tables.records.TAuthOauth2RefreshTokenRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@Disabled
class RefreshTokenGranterTest : BkCiAbstractTest() {
    private val accessTokenService = mockk<Oauth2AccessTokenService>()

    private val refreshTokenService = mockk<Oauth2RefreshTokenService>()

    private val self: RefreshTokenGranter = spyk(
        RefreshTokenGranter(
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

    private val accessTokenInfo = TAuthOauth2AccessTokenRecord().apply {
        accessToken = "testAccessToken"
        clientId = "testClientId"
        userName = "testUserName"
        grantType = "testGrantType"
        expiredTime = System.currentTimeMillis() / 1000 + 1000
        refreshToken = "testRefreshToken"
        scopeId = 1
        createTime = LocalDateTime.now()
    }

    private val accessTokenRequest = Oauth2AccessTokenRequest(
        refreshToken = "testRefreshToken",
        grantType = "testGrantType"
    )

    @Test
    fun `getAccessToken should return valid Oauth2AccessTokenDTO`() {
        val refreshTokenInfo = TAuthOauth2RefreshTokenRecord(
            "testRefreshToken",
            "testClientId",
            System.currentTimeMillis() / 1000 + 1000,
            LocalDateTime.now()
        )
        every { refreshTokenService.get(any()) } returns refreshTokenInfo
        every { accessTokenService.get(any(), any(), any(), any()) } returns accessTokenInfo
        every { accessTokenService.delete(any()) } returns Unit

        val accessTokenDto = self.getAccessToken(
            accessTokenRequest = accessTokenRequest,
            clientDetails = clientDetails
        )

        assertEquals(accessTokenInfo.userName, accessTokenDto.userName)
        assertEquals(accessTokenInfo.refreshToken, accessTokenDto.refreshToken)
        assertEquals(accessTokenInfo.scopeId, accessTokenDto.scopeId)

        verify { refreshTokenService.get(accessTokenRequest.refreshToken) }
        verify { accessTokenService.get(clientDetails.clientId, refreshToken = accessTokenRequest.refreshToken) }
        verify { accessTokenService.delete(accessTokenInfo.accessToken) }
    }

    @Test
    fun `getAccessToken should throw ErrorCodeException when refreshToken is expired`() {
        val expiredRefreshTokenInfo = TAuthOauth2RefreshTokenRecord(
            "testRefreshToken",
            "testClientId",
            System.currentTimeMillis() / 1000 - 1000,
            LocalDateTime.now()
        )
        every { refreshTokenService.get(any()) } returns expiredRefreshTokenInfo
        every { accessTokenService.get(clientId = any(), refreshToken = any()) } returns accessTokenInfo
        every { refreshTokenService.delete(any()) } returns Unit
        every { accessTokenService.delete(any()) } returns Unit

        val exception = assertThrows<ErrorCodeException> {
            self.getAccessToken(
                accessTokenRequest = accessTokenRequest,
                clientDetails = clientDetails
            )
        }

        assertEquals(ERROR_REFRESH_TOKEN_EXPIRED, exception.errorCode)
        verify { refreshTokenService.get(accessTokenRequest.refreshToken) }
        verify { refreshTokenService.delete(expiredRefreshTokenInfo.refreshToken) }
    }
}
