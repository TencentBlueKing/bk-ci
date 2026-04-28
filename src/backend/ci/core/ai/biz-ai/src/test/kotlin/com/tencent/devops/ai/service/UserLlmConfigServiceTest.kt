package com.tencent.devops.ai.service

import com.tencent.devops.ai.dao.UserLlmConfigDao
import com.tencent.devops.ai.pojo.UserLlmConfigUpsertRequest
import com.tencent.devops.model.ai.tables.records.TAiUserLlmConfigRecord
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.AESUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserLlmConfigServiceTest {

    private val dslContext = mockk<DSLContext>(relaxed = true)
    private val dao = mockk<UserLlmConfigDao>(relaxed = true)
    private val service = UserLlmConfigService(dslContext, dao, "aes-key")

    @Test
    fun `should decrypt enabled user model`() {
        every { dao.getByUserId(dslContext, "tester") } returns storedConfig(
            encryptedApiKey = AESUtil.encrypt("aes-key", "secret-key")
        )

        val model = service.getEnabledModel("tester")

        assertEquals("user-tester", model?.id)
        assertEquals("secret-key", model?.apiKey)
        assertEquals("custom-model", model?.modelName)
    }

    @Test
    fun `should reject mixed auth modes`() {
        val request = UserLlmConfigUpsertRequest(
            baseUrl = "https://llm.example.com",
            modelName = "custom-model",
            apiKey = "secret-key",
            bkAppCode = "code",
            bkAppSecret = "secret"
        )

        assertThrows(ErrorCodeException::class.java) {
            service.upsert("tester", request)
        }
    }

    @Test
    fun `should preserve existing api key when request omits secret`() {
        val captured = slot<TAiUserLlmConfigRecord>()
        every { dao.getByUserId(dslContext, "tester") } returns storedConfig(
            encryptedApiKey = AESUtil.encrypt("aes-key", "secret-key")
        )
        every { dao.upsert(dslContext, capture(captured)) } returns 1

        service.upsert(
            "tester",
            UserLlmConfigUpsertRequest(
                baseUrl = "https://llm.example.com",
                modelName = "custom-model",
                apiKey = null
            )
        )

        assertEquals(AESUtil.encrypt("aes-key", "secret-key"), captured.captured.apiKey)
        verify { dao.upsert(dslContext, any()) }
    }

    private fun storedConfig(
        encryptedApiKey: String = "",
        bkAppCode: String = "",
        encryptedBkAppSecret: String = "",
        enabled: Boolean = true
    ): TAiUserLlmConfigRecord {
        val now = LocalDateTime.now()
        return TAiUserLlmConfigRecord().apply {
            userId = "tester"
            baseUrl = "https://llm.example.com"
            modelName = "custom-model"
            apiKey = encryptedApiKey
            this.bkAppCode = bkAppCode
            bkAppSecret = encryptedBkAppSecret
            this.enabled = enabled
            connectTimeoutSeconds = 10L
            readTimeoutSeconds = 90L
            writeTimeoutSeconds = 30L
            executionTimeoutSeconds = 60L
            maxAttempts = 5
            initialBackoffSeconds = 1L
            maxBackoffSeconds = 8L
            backoffMultiplier = 2.0
            createdTime = now
            updatedTime = now
        }
    }
}
