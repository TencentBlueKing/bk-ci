package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthOauth2CodeDao
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.model.auth.tables.records.TAuthOauth2CodeRecord
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class Oauth2CodeServiceTest : BkCiAbstractTest() {
    private val authOauth2CodeDao = mockk<AuthOauth2CodeDao>()
    private val oauth2CodeService = Oauth2CodeService(
        dslContext = dslContext,
        authOauth2CodeDao = authOauth2CodeDao
    )

    @Test
    fun `test verifyCode with valid code`() {
        val clientId = "testClient"
        val codeDetails = TAuthOauth2CodeRecord(
            clientId,
            "testCode",
            "testUser",
            System.currentTimeMillis() / 1000 + 1000L,
            1,
            LocalDateTime.now()
        )

        val result = oauth2CodeService.verifyCode(clientId, codeDetails)

        assertTrue(result)
    }

    @Test
    fun `test verifyCode with invalid client id`() {
        val clientId = "testClient"
        val invalidClientId = "invalidClient"
        val codeDetails = TAuthOauth2CodeRecord(
            clientId,
            "testCode",
            "testUser",
            System.currentTimeMillis() / 1000 + 1000L,
            1,
            LocalDateTime.now()
        )

        assertThrows<ErrorCodeException> {
            oauth2CodeService.verifyCode(invalidClientId, codeDetails)
        }.apply {
            assertEquals(AuthMessageCode.INVALID_AUTHORIZATION_CODE, errorCode)
        }
    }

    @Test
    fun `test verifyCode with expired code`() {
        val clientId = "testClient"
        val codeDetails = TAuthOauth2CodeRecord(
            clientId,
            "testCode",
            "testUser",
            System.currentTimeMillis() / 1000 - 1000L,
            1,
            LocalDateTime.now()
        )

        assertThrows<ErrorCodeException> {
            oauth2CodeService.verifyCode(clientId, codeDetails)
        }.apply {
            assertEquals(AuthMessageCode.INVALID_AUTHORIZATION_EXPIRED, errorCode)
        }
    }
}
