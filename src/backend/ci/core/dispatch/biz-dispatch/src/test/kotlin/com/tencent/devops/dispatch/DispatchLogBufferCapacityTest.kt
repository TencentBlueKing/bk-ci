package com.tencent.devops.dispatch

import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.utils.DispatchLogBuffer
import com.tencent.devops.dispatch.utils.DispatchLogBuffer.DEBUG_BUFFER_LIMIT
import com.tencent.devops.dispatch.utils.DispatchLogBuffer.MAX_BUFFERED_MESSAGES
import com.tencent.devops.dispatch.utils.DispatchLogBuffer.NORMAL_BUFFER_LIMIT
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.log.meta.Ansi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DispatchLogBufferCapacityTest {
    private val printer = mockk<BuildLogPrinter>(relaxed = true)
    private val batches = mutableListOf<Pair<String, List<LogMessage>>>()
    private var lockHeld = true

    @BeforeEach
    fun setup() {
        every { printer.addLines(any(), any(), any(), any()) } answers {
            assertFalse(lockHeld, "capacity limits must never trigger a log RPC under the dispatch lock")
            batches.add(firstArg<String>() to secondArg<List<LogMessage>>())
        }
    }

    private fun <T> withBuffer(buildId: String? = "build", action: () -> T): T {
        lockHeld = true
        return DispatchLogBuffer.withBuffer(buildId, printer) {
            try {
                action()
            } finally {
                lockHeld = false
            }
        }
    }

    private fun append(
        message: String,
        type: LogType = LogType.LOG,
        buildId: String = "build",
        yellow: Boolean = false,
        red: Boolean = false
    ): Boolean = DispatchLogBuffer.append(buildId, "1", "container", 2, "job", message, type, yellow, red)

    private fun messages(): List<LogMessage> = batches.flatMap { it.second }

    // 检查锁内实际存活的集合，而非只验证最终发送数量，防止先无界收集、发送时再丢弃。
    private fun currentBuffer(): Any {
        val field = DispatchLogBuffer::class.java.getDeclaredField("current")
        field.isAccessible = true
        return (field.get(DispatchLogBuffer) as ThreadLocal<*>).get()!!
    }

    private fun field(buffer: Any, name: String): Any {
        val field = buffer.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(buffer)
    }

    private fun assertRetained(buffer: Any, builds: Int, count: Int) {
        val map = field(buffer, "messages") as Map<*, *>
        assertEquals(builds, map.size)
        assertEquals(count, map.values.sumOf { (it as Collection<*>).size })
        assertEquals(count, field(buffer, "size"))
    }

    @Test
    fun debugOverflowDoesNotRetainMoreLogsOrBuildEntries() {
        withBuffer(null) {
            repeat(5000) { assertTrue(append("node $it", LogType.DEBUG, "build-$it")) }
            assertRetained(currentBuffer(), DEBUG_BUFFER_LIMIT, DEBUG_BUFFER_LIMIT)
            assertEquals((5000 - DEBUG_BUFFER_LIMIT).toLong(), field(currentBuffer(), "droppedDebug"))
            assertTrue(batches.isEmpty())
        }
        assertEquals(DEBUG_BUFFER_LIMIT, messages().size)
        assertEquals(DEBUG_BUFFER_LIMIT, batches.map { it.first }.distinct().size)
    }

    @Test
    fun normalAndImportantMessagesKeepTheirReservedCapacityAndOrder() {
        withBuffer {
            repeat(DEBUG_BUFFER_LIMIT) { assertTrue(append("debug-$it", LogType.DEBUG)) }
            assertTrue(append("omitted debug", LogType.DEBUG))
            repeat(NORMAL_BUFFER_LIMIT - DEBUG_BUFFER_LIMIT) { assertTrue(append("normal-$it")) }
            assertTrue(append("omitted normal"))
            repeat(MAX_BUFFERED_MESSAGES - NORMAL_BUFFER_LIMIT) {
                assertTrue(append("important-$it", if (it % 2 == 0) LogType.WARN else LogType.ERROR))
            }
            assertTrue(append("omitted important", LogType.ERROR))
            val buffer = currentBuffer()
            assertRetained(buffer, 1, MAX_BUFFERED_MESSAGES)
            assertEquals(1L, field(buffer, "droppedDebug"))
            assertEquals(1L, field(buffer, "droppedNormal"))
            assertEquals(1L, field(buffer, "droppedImportant"))
        }
        val expected = (0 until DEBUG_BUFFER_LIMIT).map { "##[debug]debug-$it" } +
            (0 until NORMAL_BUFFER_LIMIT - DEBUG_BUFFER_LIMIT).map { "normal-$it" } +
            (0 until MAX_BUFFERED_MESSAGES - NORMAL_BUFFER_LIMIT).map { "important-$it" }
        assertEquals(expected, messages().map { it.message })
        assertTrue(batches.all { it.second.size <= 200 })
    }

    @Test
    fun colorsUseImportantCapacityWithoutChangingTheirOriginalFormat() {
        withBuffer {
            repeat(NORMAL_BUFFER_LIMIT) { append("normal-$it") }
            assertTrue(append("yellow", yellow = true))
            assertTrue(append("red", red = true))
            assertTrue(append("warn", LogType.WARN))
            assertTrue(append("error", LogType.ERROR))
            assertRetained(currentBuffer(), 1, NORMAL_BUFFER_LIMIT + 4)
        }
        val important = messages().takeLast(4)
        assertEquals(Ansi().bold().fgYellow().a("yellow").reset().toString(), important[0].message)
        assertEquals(Ansi().bold().fgRed().a("red").reset().toString(), important[1].message)
        assertEquals("", important[0].stepId)
        assertEquals(listOf(LogType.LOG, LogType.LOG, LogType.WARN, LogType.ERROR), important.map { it.logType })
    }

    @Test
    fun importantOnlyOverflowStillObeysTheGlobalLimit() {
        withBuffer(null) {
            repeat(MAX_BUFFERED_MESSAGES + 500) {
                assertTrue(append("error-$it", LogType.ERROR, "build-$it"))
            }
            assertTrue(append("late debug", LogType.DEBUG, "new-debug-build"))
            assertTrue(append("late normal", buildId = "new-normal-build"))
            assertRetained(currentBuffer(), MAX_BUFFERED_MESSAGES, MAX_BUFFERED_MESSAGES)
            assertEquals(500L, field(currentBuffer(), "droppedImportant"))
        }
        assertEquals(MAX_BUFFERED_MESSAGES, messages().size)
        assertTrue(messages().all { it.logType == LogType.ERROR })
    }

    @Test
    fun nestedBuildBuffersShareTheOuterEventCapacity() {
        withBuffer(null) {
            repeat(2) { build ->
                DispatchLogBuffer.withBuffer("build-$build", printer) {
                    repeat(1000) { append("debug-$it", LogType.DEBUG, "build-$build") }
                }
                assertTrue(lockHeld)
                assertTrue(batches.isEmpty(), "nested buffers must not flush or reset their budget")
            }
            assertRetained(currentBuffer(), 2, DEBUG_BUFFER_LIMIT)
        }
        assertEquals(listOf("build-0", "build-1"), batches.map { it.first }.distinct())
        assertEquals(1000, batches.filter { it.first == "build-0" }.sumOf { it.second.size })
        assertEquals(500, batches.filter { it.first == "build-1" }.sumOf { it.second.size })
        assertEquals((0 until 500).map { "##[debug]debug-$it" },
            batches.filter { it.first == "build-1" }.flatMap { it.second }.map { it.message })
    }

    @Test
    fun fullBufferStillHandlesCommonLogCallsWithoutSynchronousFallback() {
        val data = mockk<ThirdPartyAgentDispatchData>()
        every { data.buildId } returns "build"
        every { data.vmSeqId } returns "1"
        every { data.containerHashId } returns "container"
        every { data.executeCount } returns 2
        every { data.jobId } returns "job"
        val common = TPACommonUtil(mockk(relaxed = true), mockk(relaxed = true), printer)
        withBuffer {
            repeat(MAX_BUFFERED_MESSAGES) { append("important-$it", LogType.ERROR) }
            common.log(data, "normal")
            common.logDebug(data, "debug")
            common.logWarn(data, "warning")
            common.logError(data, "error")
            verify(exactly = 0) {
                printer.addLine(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
            verify(exactly = 0) {
                printer.addDebugLine(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
            verify(exactly = 0) {
                printer.addYellowLine(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
            verify(exactly = 0) {
                printer.addRedLine(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
            assertTrue(batches.isEmpty())
            assertRetained(currentBuffer(), 1, MAX_BUFFERED_MESSAGES)
        }
        assertEquals(MAX_BUFFERED_MESSAGES, messages().size)
    }

    @Test
    fun retainedLogTextIsNotTruncated() {
        val text = "service diagnostic ".repeat(1000)
        withBuffer { assertTrue(append(text)) }
        assertEquals(text, messages().single().message)
    }

    @Test
    fun rejectedBuildDoesNotConsumeTheSingleBuildBudget() {
        withBuffer {
            repeat(MAX_BUFFERED_MESSAGES) { assertFalse(append("unrelated", LogType.ERROR, "other")) }
            assertRetained(currentBuffer(), 0, 0)
            assertTrue(append("expected"))
        }
        assertEquals(listOf("expected"), messages().map { it.message })
    }

    @Test
    fun overflowDoesNotReplaceDispatchExceptionAndNextEventGetsAFreshBudget() {
        val failure = IllegalStateException("dispatch failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            withBuffer {
                repeat(DEBUG_BUFFER_LIMIT + 1) { append("debug-$it", LogType.DEBUG) }
                throw failure
            }
        }
        assertSame(failure, actual)
        assertEquals(DEBUG_BUFFER_LIMIT, messages().size)
        assertFalse(append("outside"))
        batches.clear()
        val result = withBuffer {
            repeat(NORMAL_BUFFER_LIMIT) { append("new-$it") }
            assertRetained(currentBuffer(), 1, NORMAL_BUFFER_LIMIT)
            "queued"
        }
        assertEquals("queued", result)
        assertEquals(NORMAL_BUFFER_LIMIT, messages().size)
        assertFalse(append("outside"))
    }

    @Test
    fun flushDrainsReferencesAndContinuesAfterFailedBatch() {
        var buffer: Any? = null
        var calls = 0
        every { printer.addLines(any(), any(), any(), any()) } answers {
            assertFalse(lockHeld)
            calls++
            val remaining = when (calls) {
                1 -> 206
                2 -> 6
                3 -> 1
                else -> 0
            }
            // 第一批失败也必须释放引用；发送第二个构建时，第一个构建条目已被移除。
            assertRetained(buffer!!, if (calls <= 3) 2 else 1, remaining)
            if (calls == 1) throw IllegalStateException("log unavailable")
            batches.add(firstArg<String>() to secondArg<List<LogMessage>>())
        }
        val result = withBuffer(null) {
            buffer = currentBuffer()
            repeat(405) { append(it.toString(), buildId = "build-a") }
            append("other", buildId = "build-b")
            "queued"
        }
        assertEquals("queued", result)
        assertEquals(4, calls)
        assertRetained(buffer!!, 0, 0)
        assertEquals((200..404).map { it.toString() } + "other", messages().map { it.message })
        assertFalse(append("outside"))
    }
}
