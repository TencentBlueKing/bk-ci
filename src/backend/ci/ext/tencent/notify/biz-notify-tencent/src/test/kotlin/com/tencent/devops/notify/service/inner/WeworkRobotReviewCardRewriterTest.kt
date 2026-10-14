package com.tencent.devops.notify.service.inner

import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButton
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardMainTitle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class WeworkRobotReviewCardRewriterTest {

    @Test
    fun `keeps type 0 callback key for message-push card event`() {
        val card = sampleCard()
        val rewritten = WeworkRobotReviewCardRewriter.rewriteCallbackButtons(
            card = card,
            gatewayOuter = "https://example.com/"
        )
        assertSame(card, rewritten)
        val agree = rewritten.buttonList!!.first()
        assertEquals(0, agree.type)
        assertEquals(WeworkReviewCardConst.buttonKey("agree", "review_p1"), agree.key)
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
