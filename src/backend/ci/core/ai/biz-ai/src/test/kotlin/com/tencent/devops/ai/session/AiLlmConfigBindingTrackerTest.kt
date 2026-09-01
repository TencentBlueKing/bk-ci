package com.tencent.devops.ai.session

import com.tencent.devops.ai.pojo.UserLlmConfigInfo
import com.tencent.devops.ai.service.UserLlmConfigService
import io.agentscope.spring.boot.agui.common.AguiProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiLlmConfigBindingTrackerTest {

    private val userLlmConfigService = mockk<UserLlmConfigService>()

    @Test
    fun `should return platform fingerprint when user config is absent or disabled`() {
        val tracker = createTracker()

        every { userLlmConfigService.get("absent") } returns null
        every { userLlmConfigService.get("disabled") } returns userConfig(enabled = false)

        assertEquals(AiLlmConfigBindingTracker.PLATFORM_FINGERPRINT, tracker.resolveFingerprint("absent"))
        assertEquals(AiLlmConfigBindingTracker.PLATFORM_FINGERPRINT, tracker.resolveFingerprint("disabled"))
    }

    @Test
    fun `should invalidate only when bound fingerprint changes`() {
        val tracker = createTracker()

        every { userLlmConfigService.get("tester") } returns userConfig(updatedTime = 100L)

        assertFalse(tracker.invalidateIfStale("thread-1", "tester"))

        tracker.bind("thread-1", 100L)
        assertFalse(tracker.invalidateIfStale("thread-1", "tester"))

        every { userLlmConfigService.get("tester") } returns userConfig(updatedTime = 200L)

        assertTrue(tracker.invalidateIfStale("thread-1", "tester"))
    }

    @Test
    fun `should clear binding after explicit evict`() {
        val tracker = createTracker()

        every { userLlmConfigService.get("tester") } returns userConfig(updatedTime = 200L)

        tracker.bind("thread-1", 100L)
        assertTrue(tracker.invalidateIfStale("thread-1", "tester"))

        tracker.evict("thread-1")

        assertFalse(tracker.invalidateIfStale("thread-1", "tester"))
    }

    @Test
    fun `should keep binding cache bounded by session limit`() {
        val tracker = createTracker(maxThreadSessions = 1)

        tracker.bind("thread-1", 100L)
        tracker.bind("thread-2", 200L)
        tracker.bind("thread-3", 300L)
        tracker.cleanUp()

        assertTrue(
            tracker.bindingCount() <= 2L,
            "expected binding cache size to stay within buffered maxThreadSessions"
        )
    }

    private fun createTracker(
        maxThreadSessions: Int = 100,
        sessionTimeoutMinutes: Int = 30
    ): AiLlmConfigBindingTracker {
        val aguiProperties = mockk<AguiProperties>()
        every { aguiProperties.maxThreadSessions } returns maxThreadSessions
        every { aguiProperties.sessionTimeoutMinutes } returns sessionTimeoutMinutes
        return AiLlmConfigBindingTracker(
            userLlmConfigService = userLlmConfigService,
            aguiProperties = aguiProperties
        )
    }

    private fun userConfig(
        enabled: Boolean = true,
        updatedTime: Long = 100L
    ): UserLlmConfigInfo {
        return UserLlmConfigInfo(
            userId = "tester",
            baseUrl = "https://example.com",
            modelName = "model",
            hasApiKey = true,
            bkAppCode = "",
            hasBkAppSecret = false,
            enabled = enabled,
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 90,
            writeTimeoutSeconds = 30,
            executionTimeoutSeconds = 60,
            maxAttempts = 5,
            initialBackoffSeconds = 1,
            maxBackoffSeconds = 8,
            backoffMultiplier = 2.0,
            createdTime = 1L,
            updatedTime = updatedTime
        )
    }
}
