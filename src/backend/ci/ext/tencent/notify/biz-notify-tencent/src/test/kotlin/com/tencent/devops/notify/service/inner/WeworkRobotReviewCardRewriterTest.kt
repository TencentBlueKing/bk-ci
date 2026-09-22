package com.tencent.devops.notify.service.inner

import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButton
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardMainTitle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class WeworkRobotReviewCardRewriterTest {

    @Test
    fun `blank gateway keeps callback button`() {
        val card = sampleCard()
        assertSame(card, WeworkRobotReviewCardRewriter.rewriteCallbackButtons(card, "  "))
    }

    @Test
    fun `callback button becomes jump to process external get`() {
        val rewritten = WeworkRobotReviewCardRewriter.rewriteCallbackButtons(
            card = sampleCard(),
            gatewayOuter = "https://example.com/"
        )
        val agree = rewritten.buttonList.first()
        assertEquals(1, agree.type)
        assertNull(agree.key)
        assertEquals(
            "https://example.com/ms/process/api/external/review/wework/manual_review_call_back" +
                "?action=agree&taskId=review_p1",
            agree.url
        )
        assertEquals(1, rewritten.buttonList[1].type)
        assertEquals("https://example.com/review?action=reject", rewritten.buttonList[1].url)
    }

    private fun sampleCard(): WeworkTemplateCard {
        return WeworkTemplateCard(
            mainTitle = WeworkTemplateCardMainTitle(title = "审核", desc = "desc"),
            buttonList = listOf(
                WeworkTemplateCardButton(
                    text = "通过",
                    style = 1,
                    type = 0,
                    key = WeworkReviewCardConst.buttonKey("agree", "review_p1")
                ),
                WeworkTemplateCardButton(
                    text = "驳回并填写意见",
                    style = 4,
                    type = 1,
                    url = "https://example.com/review?action=reject"
                )
            ),
            taskId = "review_p1"
        )
    }
}
