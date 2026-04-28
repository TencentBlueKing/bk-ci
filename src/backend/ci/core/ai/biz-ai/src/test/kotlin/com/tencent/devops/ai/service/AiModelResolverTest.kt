package com.tencent.devops.ai.service

import com.tencent.devops.ai.config.AiModelFactory
import com.tencent.devops.ai.model.FailoverChatModel
import com.tencent.devops.ai.properties.AiLlmModelProperties
import com.tencent.devops.ai.properties.AiLlmProperties
import io.agentscope.core.model.OpenAIChatModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiModelResolverTest {

    private val modelFactory = mockk<AiModelFactory>()
    private val userLlmConfigService = mockk<UserLlmConfigService>()

    @Test
    fun `should use user model with platform fallback when user model exists`() {
        val userConfig = AiLlmModelProperties(
            id = "user-tester",
            baseUrl = "https://user.example.com",
            modelName = "user-model",
            apiKey = "user-key"
        )
        val platformConfig = AiLlmModelProperties(
            id = "platform",
            baseUrl = "https://platform.example.com",
            modelName = "platform-model",
            apiKey = "platform-key"
        )
        val userModel = mockk<OpenAIChatModel>(relaxed = true)
        val platformModel = mockk<OpenAIChatModel>(relaxed = true)
        every { userLlmConfigService.getEnabledModel("tester") } returns userConfig
        val resolver = AiModelResolver(
            properties = AiLlmProperties(
                models = listOf(platformConfig)
            ),
            modelFactory = modelFactory,
            userLlmConfigService = userLlmConfigService
        )

        every { modelFactory.createSingleAttempt(userConfig) } returns userModel
        every { modelFactory.create(platformConfig) } returns platformModel
        val resolved = resolver.resolve("tester")

        assertEquals(AiModelSource.USER, resolved.source)
        assertEquals("user-tester -> platform", resolved.identifier)
        assertTrue(resolved.model is FailoverChatModel)
        verify(exactly = 1) { modelFactory.createSingleAttempt(userConfig) }
        verify(exactly = 1) { modelFactory.create(platformConfig) }
    }

    @Test
    fun `should build platform failover chain with single attempt candidates when user model is absent`() {
        val primaryConfig = AiLlmModelProperties(
            id = "primary",
            baseUrl = "https://primary.example.com",
            modelName = "primary-model",
            apiKey = "primary-key",
            priority = 10
        )
        val backupConfig = AiLlmModelProperties(
            id = "backup",
            baseUrl = "https://backup.example.com",
            modelName = "backup-model",
            apiKey = "backup-key",
            priority = 20
        )
        every { userLlmConfigService.getEnabledModel("tester") } returns null
        every { modelFactory.createSingleAttempt(primaryConfig) } returns mockk(relaxed = true)
        every { modelFactory.createSingleAttempt(backupConfig) } returns mockk(relaxed = true)

        val resolver = AiModelResolver(
            properties = AiLlmProperties(models = listOf(backupConfig, primaryConfig)),
            modelFactory = modelFactory,
            userLlmConfigService = userLlmConfigService
        )

        val resolved = resolver.resolve("tester")

        assertEquals(AiModelSource.PLATFORM, resolved.source)
        assertEquals("primary -> backup", resolved.identifier)
        assertTrue(resolved.model is FailoverChatModel)
        verify(exactly = 0) { modelFactory.create(any()) }
        verify(exactly = 1) { modelFactory.createSingleAttempt(primaryConfig) }
        verify(exactly = 1) { modelFactory.createSingleAttempt(backupConfig) }
    }
}
