package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import com.tencent.devops.process.pojo.pipeline.ExtServiceWeworkReviewCallBackReq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TXPipelineWeworkReviewServiceTest {

    @Test
    fun `resolve from simple fields`() {
        val req = ExtServiceWeworkReviewCallBackReq(
            userId = "royalhuang",
            action = "agree",
            taskId = "task-1"
        )
        assertEquals(
            Triple("royalhuang", WeworkReviewCardConst.ACTION_AGREE, "task-1"),
            TXPipelineWeworkReviewService.resolveExternalCallBack(req)
        )
    }

    @Test
    fun `resolve FromUserName EventKey TaskId aliases`() {
        val req = JsonUtil.to(
            """
            {
              "FromUserName": "royalhuang@example.com",
              "EventKey": "BKCI_REVIEW|approve|review_p1_b1",
              "TaskId": "review_p1_b1"
            }
            """.trimIndent(),
            ExtServiceWeworkReviewCallBackReq::class.java
        )
        assertEquals(
            Triple("royalhuang", WeworkReviewCardConst.ACTION_AGREE, "review_p1_b1"),
            TXPipelineWeworkReviewService.resolveExternalCallBack(req)
        )
    }

    @Test
    fun `resolve action and taskId from eventKey when fields missing`() {
        val req = ExtServiceWeworkReviewCallBackReq(
            userId = "fayewang",
            eventKey = "BKCI_REVIEW|reject|task-2"
        )
        assertEquals(
            Triple("fayewang", WeworkReviewCardConst.ACTION_REJECT, "task-2"),
            TXPipelineWeworkReviewService.resolveExternalCallBack(req)
        )
    }
}
