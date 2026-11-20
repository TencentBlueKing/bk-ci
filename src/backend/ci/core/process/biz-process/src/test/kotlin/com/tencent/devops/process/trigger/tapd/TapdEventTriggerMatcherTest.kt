package com.tencent.devops.process.trigger.tapd

import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerInput
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STATUS_IGNORED
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.event.TapdWebhookTriggerEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TapdEventTriggerMatcherTest {
    private val matcher = TapdEventTriggerMatcher()

    private fun element(
        includeStatuses: List<String>? = null,
        excludeStatuses: List<String>? = null
    ) = TapdWebHookTriggerElement(
        id = "task-1",
        data = TapdWebHookTriggerData(
            input = TapdWebHookTriggerInput(
                workspaceId = "70247651",
                eventType = TapdEventType.STORY,
                includeStatuses = includeStatuses,
                excludeStatuses = excludeStatuses
            )
        )
    )

    private fun event(action: TapdEventAction) = TapdWebhookTriggerEvent(
        projectId = "demo",
        pipelineId = "p-1",
        eventId = 1L,
        workspaceId = "70247651",
        eventType = TapdEventType.STORY,
        eventAction = action,
        triggerUser = "v_user"
    )

    private fun body(
        currentStatus: String? = null,
        newStatus: String? = null,
        status: String? = null
    ): Map<String, String> = buildMap {
        currentStatus?.let { put("current_status", it) }
        newStatus?.let { put("new_status", it) }
        status?.let { put("status", it) }
    }

    private fun match(
        element: TapdWebHookTriggerElement,
        body: Map<String, String>,
        action: TapdEventAction = TapdEventAction.STATUS_CHANGE,
        variables: Map<String, String> = emptyMap()
    ) = matcher.matches(element, event(action), body, variables)

    @Test
    fun `empty included statuses listens to all`() {
        val response = match(element(), body(currentStatus = "planning"), action = TapdEventAction.CREATE)
        assertEquals(MatchStatus.SUCCESS, response.matchStatus)
    }

    @Test
    fun `status filter applies to all event actions`() {
        listOf(
            TapdEventAction.CREATE,
            TapdEventAction.UPDATE,
            TapdEventAction.STATUS_CHANGE,
            TapdEventAction.ADD_COMMENT
        ).forEach { action ->
            val response = match(
                element(includeStatuses = listOf("developing")),
                body(currentStatus = "developing"),
                action = action
            )
            assertEquals(MatchStatus.SUCCESS, response.matchStatus)
        }
    }

    @Test
    fun `included status mismatch is rejected`() {
        val response = match(
            element(includeStatuses = listOf("developing")),
            body(currentStatus = "planning")
        )
        assertEquals(MatchStatus.CONDITION_NOT_MATCH, response.matchStatus)
        assertNotNull(response.failedReason)
    }

    @Test
    fun `excluded status wins over included`() {
        val response = match(
            element(
                includeStatuses = listOf("developing"),
                excludeStatuses = listOf("developing")
            ),
            body(currentStatus = "developing")
        )
        assertEquals(MatchStatus.CONDITION_NOT_MATCH, response.matchStatus)
        assertNotNull(response.failedReason)
        assertTrue(response.failedReason?.contains(BK_TAPD_STATUS_IGNORED) == true)
    }

    @Test
    fun `blank status without included keeps old behavior`() {
        val response = match(
            element(excludeStatuses = listOf("rejected")),
            body()
        )
        assertEquals(MatchStatus.SUCCESS, response.matchStatus)
    }

    @Test
    fun `blank status with included is rejected`() {
        val response = match(
            element(includeStatuses = listOf("developing")),
            body()
        )
        assertEquals(MatchStatus.CONDITION_NOT_MATCH, response.matchStatus)
        assertNotNull(response.failedReason)
    }

    @Test
    fun `new status is used when current status is absent`() {
        val response = match(
            element(includeStatuses = listOf("developing")),
            body(newStatus = "developing")
        )
        assertEquals(MatchStatus.SUCCESS, response.matchStatus)
    }

    @Test
    fun `status field is fallback for create events`() {
        val response = match(
            element(includeStatuses = listOf("planning")),
            body(status = "planning"),
            action = TapdEventAction.CREATE
        )
        assertEquals(MatchStatus.SUCCESS, response.matchStatus)
    }

    @Test
    fun `pipeline variables are resolved before matching`() {
        val response = match(
            element(includeStatuses = listOf("$" + "{targetStatus}")),
            body(currentStatus = "developing"),
            variables = mapOf("targetStatus" to "developing")
        )
        assertEquals(MatchStatus.SUCCESS, response.matchStatus)
    }
}
