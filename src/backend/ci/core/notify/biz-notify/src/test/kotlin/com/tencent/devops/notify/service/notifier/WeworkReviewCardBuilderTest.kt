package com.tencent.devops.notify.service.notifier

import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeworkReviewCardBuilderTest {

    @Test
    fun `two reviewers get distinct task ids and same layout`() {
        val plan = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang", "fayewang"),
                titleParams = mapOf("buildNum" to "6"),
                bodyParams = mapOf(
                    "projectName" to "Royal流水线测试",
                    "pipelineName" to "stage-review-notice",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewAppUrl" to "https://example.com/app",
                    "reviewDesc" to "重要审核XXX",
                    "reviewStage" to "[1]Stage审核",
                    "triggerUser" to "royalhuang"
                )
            ),
            title = "ignored-long-title",
            redisOperation = null
        )
        assertNotNull(plan)
        assertEquals(setOf("royalhuang", "fayewang"), plan!!.receiverCards.keys)
        val first = plan.receiverCards.getValue("royalhuang")
        val second = plan.receiverCards.getValue("fayewang")
        assertNotEquals(first.taskId, second.taskId)
        assertEquals("流水线人工审核（会签）", first.mainTitle.title)
        assertEquals("#6", first.horizontalContentList!!.first { it.keyname == "构建号" }.value)
        assertEquals("审核说明", first.quoteArea!!.title)
        assertEquals("通过", first.buttonList[0].text)
        assertEquals(0, first.buttonList[0].type)
        assertEquals("驳回并填写意见", first.buttonList[1].text)
        assertEquals(1, first.buttonList[1].type)
        assertTrue(first.buttonList[1].url!!.contains("action=reject"))
        assertEquals(2, first.jumpList!!.size)
    }

    @Test
    fun `required params use jump buttons only`() {
        val plan = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "buildNum" to "8",
                    "projectName" to "demo",
                    "pipelineName" to "p",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewAppUrl" to "https://example.com/app",
                    "hasRequiredParams" to "true"
                )
            ),
            title = "",
            redisOperation = null
        )
        assertNotNull(plan)
        val card = plan!!.receiverCards.getValue("royalhuang")
        assertEquals("通过并填写意见", card.buttonList[0].text)
        assertEquals(1, card.buttonList[0].type)
        assertTrue(card.buttonList[0].url!!.contains("action=approve"))
        assertEquals("驳回并填写意见", card.buttonList[1].text)
        assertEquals(1, card.buttonList[1].type)
    }

    @Test
    fun `missing callback skips card so existing text path stays`() {
        val plan = WeworkReviewCardBuilder.build(
            request = SendNotifyMessageTemplateRequest(
                templateCode = "MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE",
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf("reviewUrl" to "https://example.com/pc")
            ),
            title = "t",
            redisOperation = null
        )
        assertNull(plan)
    }

    private fun reviewRequest(
        receivers: MutableSet<String>,
        titleParams: Map<String, String> = emptyMap(),
        bodyParams: Map<String, String>
    ) = SendNotifyMessageTemplateRequest(
        templateCode = "MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE",
        receivers = receivers,
        titleParams = titleParams,
        bodyParams = bodyParams,
        callbackData = mapOf(
            "reviewType" to "STAGE",
            "projectId" to "royalpipeline",
            "pipelineId" to "p-1",
            "buildId" to "b-1",
            "stageId" to "s-1"
        )
    )
}
