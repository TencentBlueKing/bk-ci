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
        assertEquals("流水线人工审核", first.mainTitle.title)
        val rows = first.horizontalContentList!!
        assertEquals(listOf("项目", "流水线", "构建号", "审核阶段", "审核人", "触发人"), rows.map { it.keyname })
        assertEquals("Royal流水线测试", rows[0].value)
        assertEquals("stage-review-notice", rows[1].value)
        assertEquals("#6", rows[2].value)
        assertEquals("[1]Stage审核", rows[3].value)
        assertEquals("royalhuang、fayewang", rows[4].value)
        assertEquals("触发人", rows.last().keyname)
        assertEquals(3, rows.last().type)
        assertEquals("审核说明", first.quoteArea!!.title)
        assertEquals("重要审核XXX", first.quoteArea!!.quoteText)
        assertNull(first.subTitleText)
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
        assertTrue(rows.none { it.keyname == "进度" })
    }

    @Test
    fun `review progress counts groups not reviewers`() {
        val waiting = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "projectName" to "Royal流水线测试",
                    "pipelineName" to "stage-review-notice",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewStage" to "[1]Stage审核",
                    "triggerUser" to "royalhuang",
                    "buildNum" to "10",
                    "reviewers" to "royalhuang,fayewang",
                    "reviewGroups" to """
                        [{"name":"环节一","status":"","reviewers":["royalhuang","fayewang"]},
                        {"name":"环节二","status":"","reviewers":["royalhuang"]}]
                    """.trimIndent()
                )
            ),
            title = "",
            redisOperation = null
        )
        val waitingCard = waiting!!.receiverCards.getValue("royalhuang")
        assertEquals(
            listOf("项目", "流水线", "构建号", "进度", "审核人", "触发人"),
            waitingCard.horizontalContentList!!.map { it.keyname }
        )
        assertEquals("0/2", waitingCard.horizontalContentList!!.first { it.keyname == "进度" }.value)
        assertEquals("royalhuang、fayewang", waitingCard.horizontalContentList!!.first { it.keyname == "审核人" }.value)
        assertNull(waitingCard.subTitleText)

        val next = WeworkReviewCardBuilder.build(
            request = reviewRequest(
                receivers = mutableSetOf("royalhuang"),
                bodyParams = mapOf(
                    "projectName" to "Royal流水线测试",
                    "pipelineName" to "stage-review-notice",
                    "reviewUrl" to "https://example.com/pc",
                    "reviewers" to "royalhuang",
                    "reviewGroups" to """
                        [{"name":"环节一","status":"PROCESS","reviewers":["royalhuang","fayewang"]},
                        {"name":"环节二","status":"","reviewers":["royalhuang"]}]
                    """.trimIndent()
                )
            ),
            title = "",
            redisOperation = null
        )
        val nextCard = next!!.receiverCards.getValue("royalhuang")
        assertEquals("1/2", nextCard.horizontalContentList!!.first { it.keyname == "进度" }.value)
        assertTrue(nextCard.horizontalContentList!!.none { it.keyname == "审核人" })
        assertNull(nextCard.subTitleText)
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
        assertEquals("demo", card.horizontalContentList!!.first { it.keyname == "项目" }.value)
        assertTrue(card.subTitleText!!.contains("发布备注"))
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
        val card = plan!!.receiverCards.getValue("royalhuang")
        val pipeline = card.horizontalContentList!!.first { it.keyname == "流水线" }.value
        assertTrue(pipeline.length <= 30)
        assertTrue(pipeline.startsWith("a-very-long-pipeline"))
        assertTrue(pipeline.endsWith("…"))
        assertEquals("审核说明", card.quoteArea!!.title)
        assertTrue(card.quoteArea!!.quoteText!!.length <= 112)
        assertTrue(card.quoteArea!!.quoteText!!.startsWith("说明"))
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
