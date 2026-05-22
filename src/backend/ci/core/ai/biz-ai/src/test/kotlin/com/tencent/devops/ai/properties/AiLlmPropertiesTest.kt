package com.tencent.devops.ai.properties

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiLlmPropertiesTest {

    @Test
    fun `should sort enabled platform models by priority`() {
        val properties = AiLlmProperties(
            models = listOf(
                AiLlmModelOverride(
                    id = "slow-backup",
                    baseUrl = "https://backup.example.com",
                    modelName = "backup-model",
                    apiKey = "backup-key",
                    priority = 20,
                    enabled = true
                ),
                AiLlmModelOverride(
                    id = "primary",
                    baseUrl = "https://primary.example.com",
                    modelName = "primary-model",
                    apiKey = "primary-key",
                    priority = 10,
                    enabled = true
                ),
                AiLlmModelOverride(
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

    @Test
    fun `should inherit top-level defaults when override fields are absent`() {
        val properties = AiLlmProperties(
            connectTimeoutSeconds = 15,
            readTimeoutSeconds = 120,
            writeTimeoutSeconds = 45,
            executionTimeoutSeconds = 75,
            maxAttempts = 7,
            initialBackoffSeconds = 4,
            maxBackoffSeconds = 16,
            backoffMultiplier = 2.5,
            apiKey = "shared-default-key",
            models = listOf(
                AiLlmModelOverride(
                    id = "primary",
                    baseUrl = "https://primary.example.com",
                    modelName = "primary-model",
                    priority = 1
                )
            )
        )

        val effective = properties.enabledPlatformModels().single()

        assertEquals("primary", effective.id)
        assertEquals("https://primary.example.com", effective.baseUrl)
        assertEquals("primary-model", effective.modelName)
        assertEquals("shared-default-key", effective.apiKey)
        assertEquals(15, effective.connectTimeoutSeconds)
        assertEquals(120, effective.readTimeoutSeconds)
        assertEquals(45, effective.writeTimeoutSeconds)
        assertEquals(75, effective.executionTimeoutSeconds)
        assertEquals(7, effective.maxAttempts)
        assertEquals(4, effective.initialBackoffSeconds)
        assertEquals(16, effective.maxBackoffSeconds)
        assertEquals(2.5, effective.backoffMultiplier)
        assertEquals(1, effective.priority)
        assertEquals(true, effective.enabled)
    }

    @Test
    fun `should override top-level defaults when override fields are present`() {
        val properties = AiLlmProperties(
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 90,
            writeTimeoutSeconds = 30,
            executionTimeoutSeconds = 60,
            maxAttempts = 5,
            initialBackoffSeconds = 1,
            maxBackoffSeconds = 8,
            backoffMultiplier = 2.0,
            apiKey = "default-key",
            models = listOf(
                AiLlmModelOverride(
                    id = "custom",
                    baseUrl = "https://custom.example.com",
                    modelName = "custom-model",
                    apiKey = "custom-key",
                    connectTimeoutSeconds = 99,
                    readTimeoutSeconds = 199,
                    writeTimeoutSeconds = 299,
                    executionTimeoutSeconds = 399,
                    maxAttempts = 9,
                    initialBackoffSeconds = 11,
                    maxBackoffSeconds = 22,
                    backoffMultiplier = 4.0,
                    priority = 5,
                    enabled = true
                )
            )
        )

        val effective = properties.enabledPlatformModels().single()

        assertEquals("custom-key", effective.apiKey)
        assertEquals(99, effective.connectTimeoutSeconds)
        assertEquals(199, effective.readTimeoutSeconds)
        assertEquals(299, effective.writeTimeoutSeconds)
        assertEquals(399, effective.executionTimeoutSeconds)
        assertEquals(9, effective.maxAttempts)
        assertEquals(11, effective.initialBackoffSeconds)
        assertEquals(22, effective.maxBackoffSeconds)
        assertEquals(4.0, effective.backoffMultiplier)
    }
}
