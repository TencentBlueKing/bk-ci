package com.tencent.devops.ai.service

import com.tencent.devops.ai.config.AiModelFactory
import com.tencent.devops.ai.model.AiErrorClassifier
import com.tencent.devops.ai.model.FailoverChatModel
import com.tencent.devops.ai.properties.AiLlmModelOverride
import com.tencent.devops.ai.properties.AiLlmModelProperties
import com.tencent.devops.ai.properties.AiLlmProperties
import io.agentscope.core.model.OpenAIChatModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiModelResolverTest {

    private val modelFactory = mockk<AiModelFactory>()
    private val userLlmConfigService = mockk<UserLlmConfigService>()
    private val errorClassifier = AiErrorClassifier()

    @Test
    fun `should use user model with platform fallback when user model exists`() {
        val userConfig = AiLlmModelProperties(
            id = "user-tester",
            baseUrl = "https://user.example.com",
            modelName = "user-model",
            apiKey = "user-key"
        )
        val platformOverride = AiLlmModelOverride(
            id = "platform",
            baseUrl = "https://platform.example.com",
            modelName = "platform-model",
            apiKey = "platform-key"
        )
        val properties = AiLlmProperties(models = listOf(platformOverride))
        val platformEffective = platformOverride.toEffective(properties)

        val userModel = mockk<OpenAIChatModel>(relaxed = true)
        val platformModel = mockk<OpenAIChatModel>(relaxed = true)
        every { userLlmConfigService.getEnabledModel("tester") } returns userConfig

        val resolver = AiModelResolver(
            properties = properties,
            modelFactory = modelFactory,
            userLlmConfigService = userLlmConfigService,
            errorClassifier = errorClassifier
        )

        every { modelFactory.createSingleAttempt(userConfig) } returns userModel
        every { modelFactory.create(platformEffective) } returns platformModel
        val resolved = resolver.resolve("tester")

        assertEquals(AiModelSource.USER, resolved.source)
        assertEquals("user-tester -> platform", resolved.identifier)
        assertTrue(resolved.model is FailoverChatModel)
        verify(exactly = 1) { modelFactory.createSingleAttempt(userConfig) }
        verify(exactly = 1) { modelFactory.create(platformEffective) }
    }

    @Test
    fun `should build platform failover chain with single attempt candidates when user model is absent`() {
        val primaryOverride = AiLlmModelOverride(
            id = "primary",
            baseUrl = "https://primary.example.com",
            modelName = "primary-model",
            apiKey = "primary-key",
            priority = 10
        )
        val backupOverride = AiLlmModelOverride(
            id = "backup",
            baseUrl = "https://backup.example.com",
            modelName = "backup-model",
            apiKey = "backup-key",
            priority = 20
        )
        val properties = AiLlmProperties(models = listOf(backupOverride, primaryOverride))
        val primaryEffective = primaryOverride.toEffective(properties)
        val backupEffective = backupOverride.toEffective(properties)

        every { userLlmConfigService.getEnabledModel("tester") } returns null
        every { modelFactory.createSingleAttempt(primaryEffective) } returns mockk(relaxed = true)
        every { modelFactory.createSingleAttempt(backupEffective) } returns mockk(relaxed = true)

        val resolver = AiModelResolver(
            properties = properties,
            modelFactory = modelFactory,
            userLlmConfigService = userLlmConfigService,
            errorClassifier = errorClassifier
        )

        val resolved = resolver.resolve("tester")

        assertEquals(AiModelSource.PLATFORM, resolved.source)
        assertEquals("primary -> backup", resolved.identifier)
        assertTrue(resolved.model is FailoverChatModel)
        verify(exactly = 0) { modelFactory.create(any()) }
        verify(exactly = 1) { modelFactory.createSingleAttempt(primaryEffective) }
        verify(exactly = 1) { modelFactory.createSingleAttempt(backupEffective) }
    }

    @Test
    fun `should reuse underlying platform model instances across multiple resolve calls`() {
        val primaryOverride = AiLlmModelOverride(
            id = "primary",
            baseUrl = "https://primary.example.com",
            modelName = "primary-model",
            apiKey = "primary-key",
            priority = 10
        )
        val backupOverride = AiLlmModelOverride(
            id = "backup",
            baseUrl = "https://backup.example.com",
            modelName = "backup-model",
            apiKey = "backup-key",
            priority = 20
        )
        val properties = AiLlmProperties(models = listOf(backupOverride, primaryOverride))
        val primaryEffective = primaryOverride.toEffective(properties)
        val backupEffective = backupOverride.toEffective(properties)

        every { userLlmConfigService.getEnabledModel("tester") } returns null
        every { modelFactory.createSingleAttempt(primaryEffective) } returns mockk(relaxed = true)
        every { modelFactory.createSingleAttempt(backupEffective) } returns mockk(relaxed = true)

        val resolver = AiModelResolver(
            properties = properties,
            modelFactory = modelFactory,
            userLlmConfigService = userLlmConfigService,
            errorClassifier = errorClassifier
        )

        val first = resolver.resolve("tester")
        val second = resolver.resolve("tester")
        val third = resolver.resolve("tester")

        assertEquals("primary -> backup", first.identifier)
        assertEquals("primary -> backup", second.identifier)
        assertEquals("primary -> backup", third.identifier)
        assertNotSame(
            first.model,
            second.model,
            "FailoverChatModel wrapper should be rebuilt per resolve"
        )
        verify(exactly = 1) { modelFactory.createSingleAttempt(primaryEffective) }
        verify(exactly = 1) { modelFactory.createSingleAttempt(backupEffective) }
    }

    @Test
    fun `should randomize order across multiple resolve calls for same priority models`() {
        val aOverride = AiLlmModelOverride(
            id = "a",
            baseUrl = "https://a.example.com",
            modelName = "a-model",
            apiKey = "a-key",
            priority = 1
        )
        val bOverride = AiLlmModelOverride(
            id = "b",
            baseUrl = "https://b.example.com",
            modelName = "b-model",
            apiKey = "b-key",
            priority = 1
        )
        val properties = AiLlmProperties(models = listOf(aOverride, bOverride))
        val aEffective = aOverride.toEffective(properties)
        val bEffective = bOverride.toEffective(properties)

        every { userLlmConfigService.getEnabledModel("tester") } returns null
        every { modelFactory.createSingleAttempt(aEffective) } returns mockk(relaxed = true)
        every { modelFactory.createSingleAttempt(bEffective) } returns mockk(relaxed = true)

        val resolver = AiModelResolver(
            properties = properties,
            modelFactory = modelFactory,
            userLlmConfigService = userLlmConfigService,
            errorClassifier = errorClassifier,
            random = Random(42)
        )

        val observedIdentifiers = (1..50)
            .map { resolver.resolve("tester").identifier }
            .toSet()

        assertTrue(
            observedIdentifiers.contains("a -> b"),
            "expected to observe 'a -> b' across resolves, got $observedIdentifiers"
        )
        assertTrue(
            observedIdentifiers.contains("b -> a"),
            "expected to observe 'b -> a' across resolves, got $observedIdentifiers"
        )
        verify(exactly = 1) { modelFactory.createSingleAttempt(aEffective) }
        verify(exactly = 1) { modelFactory.createSingleAttempt(bEffective) }
    }
}
