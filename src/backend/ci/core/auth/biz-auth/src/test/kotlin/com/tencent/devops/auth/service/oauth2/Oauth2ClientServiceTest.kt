import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthOauth2ClientDetailsDao
import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.service.oauth2.Oauth2ClientService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.test.BkCiAbstractTest
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Disabled
class Oauth2ClientServiceTest : BkCiAbstractTest() {

    private val authOauth2ClientDetailsDao = mockk<AuthOauth2ClientDetailsDao>()

    private val oauth2ClientService = Oauth2ClientService(
        dslContext = dslContext,
        authOauth2ClientDetailsDao = authOauth2ClientDetailsDao
    )
    private val clientId = "testClient"

    private val clientDetails = ClientDetailsInfo(
        clientId = "testClient",
        clientSecret = "testSecret",
        clientName = "Test Client",
        scope = "read,write",
        authorizedGrantTypes = "authorization_code,refresh_token",
        redirectUri = "http://example.com/callback,http://example1.com/callback",
        accessTokenValidity = 3600,
        refreshTokenValidity = 86400,
        icon = "icon"
    )

    @Test
    fun `test verifyClientInformation with invalid grant type`() {
        val invalidGrantType = "invalid_grant_type"

        val exception = assertThrows<ErrorCodeException> {
            oauth2ClientService.verifyClientInformation(
                clientId = clientId,
                clientDetails = clientDetails,
                grantType = invalidGrantType
            )
        }

        assertEquals(AuthMessageCode.INVALID_AUTHORIZATION_TYPE, exception.errorCode)
        assertArrayEquals(arrayOf(clientId), exception.params)
    }

    @Test
    fun `test verifyClientInformation with invalid redirect URI`() {
        val invalidRedirectUri = "http://invalid.com/callback"

        val exception = assertThrows<ErrorCodeException> {
            oauth2ClientService.verifyClientInformation(
                clientId = clientId,
                clientDetails = clientDetails,
                redirectUri = invalidRedirectUri
            )
        }

        assertEquals(AuthMessageCode.INVALID_REDIRECT_URI, exception.errorCode)
        assertArrayEquals(arrayOf(invalidRedirectUri), exception.params)
    }

    @Test
    fun `test verifyClientInformation with invalid client secret`() {
        val invalidClientSecret = "invalidSecret"

        val exception = assertThrows<ErrorCodeException> {
            oauth2ClientService.verifyClientInformation(
                clientId = clientId,
                clientDetails = clientDetails,
                clientSecret = invalidClientSecret
            )
        }

        assertEquals(AuthMessageCode.INVALID_CLIENT_SECRET, exception.errorCode)
        assertArrayEquals(arrayOf(clientId), exception.params)
    }

    @Test
    fun `test verifyClientInformation with invalid scope`() {
        val invalidScope = listOf("invalidScope")

        val exception = assertThrows<ErrorCodeException> {
            oauth2ClientService.verifyClientInformation(
                clientId = clientId,
                clientDetails = clientDetails,
                scope = invalidScope
            )
        }

        assertEquals(AuthMessageCode.INVALID_SCOPE, exception.errorCode)
        assertArrayEquals(arrayOf(clientId), exception.params)
    }
}
