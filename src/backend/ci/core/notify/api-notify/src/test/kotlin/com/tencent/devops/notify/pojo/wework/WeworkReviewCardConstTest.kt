package com.tencent.devops.notify.pojo.wework

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeworkReviewCardConstTest {

    @Test
    fun `taskId is unique per receiver and only uses allowed chars`() {
        val first = WeworkReviewCardConst.buildTaskId("p1", "b-1", "s-1", "royalhuang", "aaa111")
        val second = WeworkReviewCardConst.buildTaskId("p1", "b-1", "s-1", "fayewang", "aaa111")
        assertNotEquals(first, second)
        assertTrue(first.startsWith("review_"))
        assertTrue(first.matches(Regex("[A-Za-z0-9_\\-@]+")))
        assertTrue(first.length <= 128)
    }

    @Test
    fun `appendUrlAction keeps existing query and skips when already present`() {
        assertEquals(
            "https://example.com/review?action=reject",
            WeworkReviewCardConst.appendUrlAction("https://example.com/review", "reject")
        )
        assertEquals(
            "https://example.com/review?x=1&action=reject",
            WeworkReviewCardConst.appendUrlAction("https://example.com/review?x=1", "reject")
        )
        assertEquals(
            "https://example.com/review?action=approve",
            WeworkReviewCardConst.appendUrlAction("https://example.com/review?action=approve", "reject")
        )
    }

    @Test
    fun `parseButtonKey accepts approve alias`() {
        val parsed = WeworkReviewCardConst.parseButtonKey("BKCI_REVIEW|approve|task-1")
        assertEquals(WeworkReviewCardConst.ACTION_AGREE to "task-1", parsed)
    }
}
