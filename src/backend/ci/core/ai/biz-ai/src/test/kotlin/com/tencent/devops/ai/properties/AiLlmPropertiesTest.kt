package com.tencent.devops.ai.properties

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiLlmPropertiesTest {

    @Test
    fun `should sort enabled platform models by priority`() {
        val properties = AiLlmProperties(
            models = listOf(
                AiLlmModelProperties(
                    id = "slow-backup",
                    baseUrl = "https://backup.example.com",
                    modelName = "backup-model",
                    apiKey = "backup-key",
                    priority = 20,
                    enabled = true
                ),
                AiLlmModelProperties(
                    id = "primary",
                    baseUrl = "https://primary.example.com",
                    modelName = "primary-model",
                    apiKey = "primary-key",
                    priority = 10,
                    enabled = true
                ),
                AiLlmModelProperties(
                    id = "disabled",
                    baseUrl = "https://disabled.example.com",
                    modelName = "disabled-model",
                    apiKey = "disabled-key",
                    priority = 0,
                    enabled = false
                )
            )
        )

        assertEquals(
            listOf("primary", "slow-backup"),
            properties.enabledPlatformModels().map { it.id }
        )
    }

    @Test
    fun `should fallback to legacy single model config when models list is empty`() {
        val properties = AiLlmProperties(
            baseUrl = "https://legacy.example.com",
            modelName = "legacy-model",
            apiKey = "legacy-key",
            connectTimeoutSeconds = 12,
            readTimeoutSeconds = 34,
            writeTimeoutSeconds = 56,
            executionTimeoutSeconds = 78,
            maxAttempts = 4,
            initialBackoffSeconds = 2,
            maxBackoffSeconds = 9,
            backoffMultiplier = 3.0
        )

        val model = properties.enabledPlatformModels().single()

        assertEquals("default", model.id)
        assertEquals("https://legacy.example.com", model.baseUrl)
        assertEquals("legacy-model", model.modelName)
        assertEquals("legacy-key", model.apiKey)
        assertEquals(12, model.connectTimeoutSeconds)
        assertEquals(34, model.readTimeoutSeconds)
        assertEquals(56, model.writeTimeoutSeconds)
        assertEquals(78, model.executionTimeoutSeconds)
        assertEquals(4, model.maxAttempts)
        assertEquals(2, model.initialBackoffSeconds)
        assertEquals(9, model.maxBackoffSeconds)
        assertEquals(3.0, model.backoffMultiplier)
    }
}
