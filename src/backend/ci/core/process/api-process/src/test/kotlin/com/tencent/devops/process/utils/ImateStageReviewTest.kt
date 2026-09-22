package com.tencent.devops.process.utils

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.StageReviewGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImateStageReviewTest {

    @Test
    fun `toAgent2UserKey strips OpenClaw prefix`() {
        val full = "agent:main:imate-bk-channel:direct:royalhuang|ins-xxx|convhash|"
        assertEquals("royalhuang|ins-xxx|convhash|", ImateStageReview.toAgent2UserKey(full))
    }

    @Test
    fun `toAgent2UserKey keeps already-normalized key`() {
        val key = "royalhuang|ins-xxx|convhash|"
        assertEquals(key, ImateStageReview.toAgent2UserKey(key))
    }

    @Test
    fun `conversationId is third segment of agent2UserKey not the whole key`() {
        val key = "royalhuang|ins-xxx|convhash|"
        assertEquals("convhash", ImateStageReview.conversationIdOf(key))
        assertFalse(key == ImateStageReview.conversationIdOf(key))
    }

    @Test
    fun `sessionMatches compares agent2UserKey not conversationId`() {
        val bound = "agent:main:direct:royalhuang|ins-xxx|convhash|"
        val cds = "royalhuang|ins-xxx|convhash|"
        assertTrue(ImateStageReview.sessionMatches(bound, cds))
        assertFalse(ImateStageReview.sessionMatches(bound, "convhash"))
        assertFalse(ImateStageReview.sessionMatches(bound, "other|ins-xxx|convhash|"))
    }

    @Test
    fun `needLock requires creative stream plus IMATE group plus session`() {
        assertTrue(
            ImateStageReview.needLock(
                channelCode = ChannelCode.CREATIVE_STREAM,
                groupName = "IMATE",
                imateSessionId = "u|g|c|"
            )
        )
        assertFalse(
            ImateStageReview.needLock(
                channelCode = ChannelCode.BS,
                groupName = "IMATE",
                imateSessionId = "u|g|c|"
            )
        )
        assertFalse(
            ImateStageReview.needLock(
                channelCode = ChannelCode.CREATIVE_STREAM,
                groupName = "发布负责人",
                imateSessionId = "u|g|c|"
            )
        )
        assertFalse(
            ImateStageReview.needLock(
                channelCode = ChannelCode.CREATIVE_STREAM,
                groupName = "IMATE",
                imateSessionId = "  "
            )
        )
    }

    @Test
    fun `taskId round trip includes projectId`() {
        val taskId = ImateStageReview.taskId("_royalhuang", "b-abc", "stage-2", 1)
        assertEquals("cs-stage~_royalhuang~b-abc~stage-2~1", taskId)
        val parts = ImateStageReview.parseTaskId(taskId)!!
        assertEquals("_royalhuang", parts.projectId)
        assertEquals("b-abc", parts.buildId)
        assertEquals("stage-2", parts.stageId)
        assertEquals(1, parts.executeCount)
        assertNull(ImateStageReview.parseTaskId("not-a-task"))
    }

    @Test
    fun `parseTaskId accepts legacy pipe and url-encoded separators`() {
        val expected = ImateStageReview.TaskIdParts("_royalhuang", "b-abc", "stage-2", 1)
        assertEquals(expected, ImateStageReview.parseTaskId("cs-stage|_royalhuang|b-abc|stage-2|1"))
        assertEquals(
            expected,
            ImateStageReview.parseTaskId("cs-stage%7E_royalhuang%7Eb-abc%7Estage-2%7E1")
        )
        assertEquals(
            expected,
            ImateStageReview.parseTaskId("cs-stage%7C_royalhuang%7Cb-abc%7Cstage-2%7C1")
        )
    }

    @Test
    fun `isImateGroup is exact name`() {
        assertTrue(ImateStageReview.isImateGroup(StageReviewGroup(name = "IMATE")))
        assertFalse(ImateStageReview.isImateGroup(StageReviewGroup(name = "imate")))
        assertFalse(ImateStageReview.isImateGroup(StageReviewGroup(name = "IMATE审核")))
    }

    @Test
    fun `decideLock pass only when status and session match`() {
        val bound = "u|g|c|"
        assertEquals(
            ImateLockDecision.PASS,
            ImateStageReview.decideLock(
                configured = true,
                queryFailed = false,
                notFound = false,
                isCancel = false,
                cdsStatus = "APPROVED",
                cdsSessionKey = bound,
                boundSessionId = "agent:main:$bound"
            )
        )
        assertEquals(
            ImateLockDecision.PASS,
            ImateStageReview.decideLock(
                configured = true,
                queryFailed = false,
                notFound = false,
                isCancel = true,
                cdsStatus = "REJECTED",
                cdsSessionKey = bound,
                boundSessionId = bound
            )
        )
        assertEquals(
            ImateLockDecision.REJECTED,
            ImateStageReview.decideLock(
                configured = true,
                queryFailed = false,
                notFound = false,
                isCancel = false,
                cdsStatus = "REJECTED",
                cdsSessionKey = bound,
                boundSessionId = bound
            )
        )
        assertEquals(
            ImateLockDecision.NOT_LOCKED,
            ImateStageReview.decideLock(
                configured = true,
                queryFailed = false,
                notFound = true,
                isCancel = false,
                cdsStatus = null,
                cdsSessionKey = null,
                boundSessionId = bound
            )
        )
        assertEquals(
            ImateLockDecision.SESSION_MISMATCH,
            ImateStageReview.decideLock(
                configured = true,
                queryFailed = false,
                notFound = false,
                isCancel = false,
                cdsStatus = "APPROVED",
                cdsSessionKey = "other|g|c|",
                boundSessionId = bound
            )
        )
        assertEquals(
            ImateLockDecision.NOT_CONFIGURED,
            ImateStageReview.decideLock(
                configured = false,
                queryFailed = false,
                notFound = false,
                isCancel = false,
                cdsStatus = "APPROVED",
                cdsSessionKey = bound,
                boundSessionId = bound
            )
        )
    }
}
