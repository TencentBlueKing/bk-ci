package com.tencent.devops.process.trigger.tapd

import com.tencent.devops.common.pipeline.enums.TapdEventAction
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerInput
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STATUS_IGNORED
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.BK_TAPD_STATUS_MAPPING_FAILED
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
        currentStatusName: String? = null,
        rawStatus: String? = null,
        mappingFailed: Boolean = false
    ): Map<String, String> = buildMap {
        currentStatusName?.let { put("current_status_name", it) }
        rawStatus?.let { put("current_status", it) }
        if (mappingFailed) put("tapd_status_mapping_failed", "true")
    }

    private fun match(
        element: TapdWebHookTriggerElement,
        body: Map<String, String>,
        action: TapdEventAction = TapdEventAction.STATUS_CHANGE,
        variables: Map<String, String> = emptyMap()
    ) = matcher.matches(element, event(action), body, variables)

    @Test
    fun `empty included statuses listens to all`() {
        val response = match(
            element(),
            body(currentStatusName = "规划中", rawStatus = "planning"),
            action = TapdEventAction.CREATE
        )
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
                element(includeStatuses = listOf("实现中")),
                body(currentStatusName = "实现中", rawStatus = "developing"),
                action = action
            )
            assertEquals(MatchStatus.SUCCESS, response.matchStatus)
        }
    }

    @Test
    fun `included status mismatch is rejected`() {
        val response = match(
            element(includeStatuses = listOf("实现中")),
            body(currentStatusName = "规划中", rawStatus = "planning")
        )
        assertEquals(MatchStatus.CONDITION_NOT_MATCH, response.matchStatus)
        assertNotNull(response.failedReason)
    }

    @Test
    fun `excluded status wins over included`() {
        val response = match(
            element(
                includeStatuses = listOf("实现中"),
                excludeStatuses = listOf("实现中")
            ),
            body(currentStatusName = "实现中", rawStatus = "developing")
        )
        assertEquals(MatchStatus.CONDITION_NOT_MATCH, response.matchStatus)
        assertTrue(response.failedReason?.contains(BK_TAPD_STATUS_IGNORED) == true)
    }

    @Test
    fun `blank status without included keeps old behavior`() {
        val response = match(
            element(excludeStatuses = listOf("已拒绝")),
            body()
        )
        assertEquals(MatchStatus.SUCCESS, response.matchStatus)
    }

    @Test
    fun `blank status with included is rejected`() {
        val response = match(
            element(includeStatuses = listOf("实现中")),
            body()
        )
        assertEquals(MatchStatus.CONDITION_NOT_MATCH, response.matchStatus)
        assertNotNull(response.failedReason)
    }

    @Test
    fun `status mapping failure is recorded as condition not match`() {
        val response = match(
            element(includeStatuses = listOf("实现中")),
            body(rawStatus = "developing", mappingFailed = true)
        )
        assertEquals(MatchStatus.CONDITION_NOT_MATCH, response.matchStatus)
        assertTrue(response.failedReason?.contains(BK_TAPD_STATUS_MAPPING_FAILED) == true)
    }

    @Test
    fun `pipeline variables are resolved before matching`() {
        val response = match(
            element(includeStatuses = listOf("$" + "{targetStatus}")),
            body(currentStatusName = "实现中", rawStatus = "developing"),
            variables = mapOf("targetStatus" to "实现中")
        )
        assertEquals(MatchStatus.SUCCESS, response.matchStatus)
    }
}
