package com.tencent.devops.ai.util

import io.agentscope.core.agui.event.AguiEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReasoningCompensationTrackerTest {

    private val threadId = "thread-1"
    private val runId = "run-1"

    @Test
    fun `should not compensate when text message exists`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        tracker.track(
            AguiEvent.TextMessageStart(threadId, runId, "m1", "assistant")
        )
        tracker.track(
            AguiEvent.TextMessageContent(threadId, runId, "m1", "hello")
        )
        tracker.track(
            AguiEvent.TextMessageEnd(threadId, runId, "m1")
        )

        assertFalse(tracker.needsCompensation())
    }

    @Test
    fun `should compensate when only reasoning messages exist`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r1", "Let me think"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r1", " about this"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r1")
        )

        assertTrue(tracker.needsCompensation())

        val events = tracker.buildCompensationEvents()
        assertEquals(3, events.size)

        val start = events[0] as AguiEvent.TextMessageStart
        assertEquals("assistant", start.role)
        assertEquals(threadId, start.threadId)
        assertEquals(runId, start.runId)

        val content = events[1] as AguiEvent.TextMessageContent
        assertEquals("Let me think about this", content.delta)
        assertEquals(start.messageId, content.messageId)

        val end = events[2] as AguiEvent.TextMessageEnd
        assertEquals(start.messageId, end.messageId)
    }

    @Test
    fun `should not compensate when reasoning has no content`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r1")
        )

        assertFalse(tracker.needsCompensation())
    }

    @Test
    fun `should not compensate when both reasoning and text exist`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r1", "thinking..."
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r1")
        )
        tracker.track(
            AguiEvent.TextMessageStart(
                threadId, runId, "m1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.TextMessageContent(
                threadId, runId, "m1", "final answer"
            )
        )
        tracker.track(
            AguiEvent.TextMessageEnd(threadId, runId, "m1")
        )

        assertFalse(tracker.needsCompensation())
    }

    @Test
    fun `compensation events should have unique messageId`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r1", "content"
            )
        )

        val events = tracker.buildCompensationEvents()
        val msgId = (events[0] as AguiEvent.TextMessageStart).messageId
        assertTrue(msgId != "r1")
        assertTrue(msgId.isNotBlank())
    }

    @Test
    fun `should not compensate when no events tracked`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        assertFalse(tracker.needsCompensation())
    }

    @Test
    fun `should accumulate multiple reasoning deltas`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        val deltas = listOf("Part1 ", "Part2 ", "Part3")
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r1", "assistant"
            )
        )
        deltas.forEach { delta ->
            tracker.track(
                AguiEvent.ReasoningMessageContent(
                    threadId, runId, "r1", delta
                )
            )
        }

        assertTrue(tracker.needsCompensation())
        val content =
            tracker.buildCompensationEvents()[1] as AguiEvent.TextMessageContent
        assertEquals("Part1 Part2 Part3", content.delta)
    }

    @Test
    fun `should ignore unrelated event types`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)
        tracker.track(AguiEvent.RunStarted(threadId, runId))
        tracker.track(
            AguiEvent.ToolCallStart(threadId, runId, "tc1", "myTool")
        )
        tracker.track(AguiEvent.RunFinished(threadId, runId))

        assertFalse(tracker.needsCompensation())
    }

    @Test
    fun `should only keep last reasoning round in multi-round ReAct`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)

        // Round 1: reasoning about tool call
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r1", "I should call tool X"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r1")
        )
        // tool call + result happen here (ToolCall* events)
        tracker.track(
            AguiEvent.ToolCallStart(
                threadId, runId, "tc1", "toolX"
            )
        )

        // Round 2: reasoning about second tool call
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r2", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r2", "Now call tool Y"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r2")
        )

        // Round 3: final answer (reasoning only, no TextBlock)
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r3", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r3", "The final answer is 42"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r3")
        )

        assertTrue(tracker.needsCompensation())
        val content = tracker.buildCompensationEvents()[1]
            as AguiEvent.TextMessageContent
        assertEquals("The final answer is 42", content.delta)
    }

    @Test
    fun `should not compensate multi-round when text message appears`() {
        val tracker = ReasoningCompensationTracker(threadId, runId)

        // Round 1: reasoning + tool call
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r1", "thinking about tool"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r1")
        )

        // Round 2: final answer with proper TextBlock
        tracker.track(
            AguiEvent.ReasoningMessageStart(
                threadId, runId, "r2", "assistant"
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageContent(
                threadId, runId, "r2", "summarizing..."
            )
        )
        tracker.track(
            AguiEvent.ReasoningMessageEnd(threadId, runId, "r2")
        )
        tracker.track(
            AguiEvent.TextMessageStart(
                threadId, runId, "m1", "assistant"
            )
        )
        tracker.track(
            AguiEvent.TextMessageContent(
                threadId, runId, "m1", "Here is the result"
            )
        )
        tracker.track(
            AguiEvent.TextMessageEnd(threadId, runId, "m1")
        )

        assertFalse(tracker.needsCompensation())
    }
}
