package com.tencent.devops.common.webhook.service.code.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TapdStatusFilterTest {
    private val includedReason = "included-reason"
    private val excludedReason = "excluded-reason"

    private fun filter(
        triggerOn: String,
        included: List<String>,
        excluded: List<String>
    ) = TapdStatusFilter(
        pipelineId = "p-1",
        triggerOn = triggerOn,
        included = included,
        excluded = excluded,
        includedFailedReason = includedReason,
        excludedFailedReason = excludedReason
    )

    @Test
    fun `empty included listens to all statuses`() {
        val response = WebhookFilterResponse()
        assertTrue(filter("developing", emptyList(), emptyList()).doFilter(response))
        assertNull(response.failedReason)
    }

    @Test
    fun `included status matches`() {
        val response = WebhookFilterResponse()
        assertTrue(filter("developing", listOf("developing"), emptyList()).doFilter(response))
    }

    @Test
    fun `included status mismatch is rejected`() {
        val response = WebhookFilterResponse()
        assertFalse(filter("planning", listOf("developing"), emptyList()).doFilter(response))
        assertEquals(includedReason, response.failedReason)
    }

    @Test
    fun `excluded status wins over included`() {
        val response = WebhookFilterResponse()
        assertFalse(filter("developing", listOf("developing"), listOf("developing")).doFilter(response))
        assertEquals(excludedReason, response.failedReason)
    }

    @Test
    fun `blank status without included keeps old behavior`() {
        val response = WebhookFilterResponse()
        assertTrue(filter("", emptyList(), listOf("rejected")).doFilter(response))
    }

    @Test
    fun `blank status with included is rejected`() {
        val response = WebhookFilterResponse()
        assertFalse(filter("", listOf("developing"), emptyList()).doFilter(response))
        assertEquals(includedReason, response.failedReason)
    }
}
