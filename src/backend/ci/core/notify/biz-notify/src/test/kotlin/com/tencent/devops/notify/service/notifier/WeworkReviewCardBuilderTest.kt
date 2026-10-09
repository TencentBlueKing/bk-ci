package com.tencent.devops.notify.service.notifier

import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
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
        assertTrue(first.subTitleText!!.contains("项目: Royal流水线测试"))
        assertTrue(first.subTitleText!!.contains("流水线: stage-review-notice"))
        assertTrue(first.subTitleText!!.contains("构建号: #6"))
        assertTrue(first.subTitleText!!.contains("审核说明: 重要审核XXX"))
        assertEquals("触发人", first.horizontalContentList!!.single().keyname)
        assertEquals(3, first.horizontalContentList!!.single().type)
        assertNull(first.quoteArea)
        assertEquals("通过", first.buttonList!![0].text)
        assertEquals(0, first.buttonList!![0].type)
        assertEquals("驳回", first.buttonList!![1].text)
        assertEquals(1, first.buttonList!![1].type)
        assertTrue(first.buttonList!![1].url!!.contains("action=reject"))
        assertEquals("查看详情", first.buttonList!![2].text)
        assertEquals(1, first.buttonList!![2].type)
        assertEquals("https://example.com/pc", first.buttonList!![2].url)
        assertNull(first.jumpList)
        assertNotNull(first.cardAction)
        assertTrue(first.subTitleText!!.contains("审批进度"))
    }

    @Test
    fun `suggest required uses jump buttons only`() {
        val plan = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "buildNum" to "8",
                    "projectName" to "demo",
                    "pipelineName" to "p",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewAppUrl" to "https://example.com/app",
                    "suggestRequired" to "true"
                )
            ),
            title = "",
            redisOperation = null
        )
        assertNotNull(plan)
        val card = plan!!.receiverCards.getValue("royalhuang")
        assertEquals("通过", card.buttonList!![0].text)
        assertEquals(1, card.buttonList!![0].type)
        assertTrue(card.buttonList!![0].url!!.contains("action=approve"))
        assertEquals("驳回", card.buttonList!![1].text)
        assertEquals(1, card.buttonList!![1].type)
        assertEquals("查看详情", card.buttonList!![2].text)
    }

    @Test
    fun `single enum param uses button selection`() {
        val plan = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "projectName" to "demo",
                    "pipelineName" to "p",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewParams" to """
                        [{"key":"deploy_env","value":"staging","valueType":"enum","required":false,
                        "chineseName":"发布环境","options":[
                        {"key":"gray","value":"灰度环境"},
                        {"key":"staging","value":"预发布环境"}]}]
                    """.trimIndent()
                )
            ),
            title = "",
            redisOperation = null
        )
        val card = plan!!.receiverCards.getValue("royalhuang")
        assertEquals(WeworkReviewCardConst.CARD_TYPE_BUTTON, card.cardType)
        assertEquals("发布环境", card.buttonSelection!!.title)
        assertEquals("staging", card.buttonSelection!!.selectedId)
        assertEquals("通过", card.buttonList!![0].text)
        assertEquals(0, card.buttonList!![0].type)
    }

    @Test
    fun `two enum params use multiple interaction`() {
        val plan = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "projectName" to "demo",
                    "pipelineName" to "p",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewParams" to """
                        [{"key":"count","value":"8","valueType":"enum","chineseName":"实例数",
                        "options":[{"key":"5","value":"5"},{"key":"8","value":"8"}]},
                        {"key":"region","value":"sh","valueType":"enum","chineseName":"区域",
                        "options":[{"key":"sh","value":"上海"},{"key":"gz","value":"广州"}]}]
                    """.trimIndent()
                )
            ),
            title = "",
            redisOperation = null
        )
        val card = plan!!.receiverCards.getValue("royalhuang")
        assertEquals(WeworkReviewCardConst.CARD_TYPE_MULTIPLE, card.cardType)
        assertEquals(2, card.selectList!!.size)
        assertEquals("参数确认无误，提交", card.submitButton!!.text)
        assertNull(card.buttonList)
        assertTrue(card.jumpList!!.any { it.title.contains("驳回") })
    }

    @Test
    fun `input params use readonly confirm card`() {
        val plan = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "projectName" to "demo",
                    "pipelineName" to "p",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewParams" to """
                        [{"key":"remark","value":"灰度观察","valueType":"string","chineseName":"发布备注"}]
                    """.trimIndent()
                )
            ),
            title = "",
            redisOperation = null
        )
        val card = plan!!.receiverCards.getValue("royalhuang")
        assertEquals(WeworkReviewCardConst.CARD_TYPE_BUTTON, card.cardType)
        assertTrue(card.subTitleText!!.contains("发布备注"))
        assertTrue(card.subTitleText!!.contains("项目: demo"))
        assertEquals("确认提交", card.buttonList!![0].text)
        assertEquals("修改参数", card.buttonList!![1].text)
        assertTrue(card.buttonList!![1].url!!.contains("action=modify"))
        assertEquals("查看详情", card.buttonList!!.last().text)
    }

    @Test
    fun `long review text keeps identity lines inside body limit`() {
        val plan = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "projectName" to "demo",
                    "pipelineName" to "a-very-long-pipeline-name-that-used-to-be-clipped",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewDesc" to "说明".repeat(200)
                )
            ),
            title = "",
            redisOperation = null
        )
        val body = plan!!.receiverCards.getValue("royalhuang").subTitleText!!
        assertTrue(body.length <= 256)
        assertTrue(body.contains("流水线: a-very-long-pipeline-name-that-used-to-be-clipped"))
        assertTrue(body.contains("审核说明:"))
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
