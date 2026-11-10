package com.tencent.devops.dispatch

import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.dao.TPAQueueDao
import com.tencent.devops.dispatch.dao.ThirdPartyAgentQueueSqlData
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.EnvQueueContext
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.TPAQueueEvent
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentSqlQueueType
import com.tencent.devops.dispatch.service.tpaqueue.TPAEnvQueueService
import com.tencent.devops.dispatch.service.tpaqueue.TPAQueueService
import com.tencent.devops.dispatch.utils.DispatchLogBuffer
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.log.meta.Ansi
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TPAQueueLogBufferTest {
    private val redis = mockk<RedisOperation>(relaxed = true)
    private val printer = mockk<BuildLogPrinter>(relaxed = true)
    private val dao = mockk<TPAQueueDao>(relaxed = true)
    private val envQueue = mockk<TPAEnvQueueService>(relaxed = true)
    private val dispatch = mockk<DispatchService>(relaxed = true)
    private val common = TPACommonUtil(mockk(relaxed = true), mockk(relaxed = true), printer)
    private val queue = TPAQueueService(
        mockk(), redis, mockk(relaxed = true), common, dao, envQueue, dispatch, mockk(), printer
    )
    private val event = TPAQueueEvent("consumer", "pipeline", "env", ThirdPartyAgentSqlQueueType.ENV, null, 0, "lock")
    private val batches = mutableListOf<Pair<String, List<LogMessage>>>()
    private var queueLockHeld = true

    @BeforeEach
    fun setup() {
        mockkObject(I18nUtil)
        every { I18nUtil.getDefaultLocaleLanguage() } returns "zh_CN"
        every { I18nUtil.getCodeLanMessage(any(), any(), any(), any(), any(), any()) } answers { firstArg() }
        every { redis.getKeyByRedisName(any()) } answers { firstArg() }
        every { redis.execute<Long>(any(), any(), *anyVararg(), isRedisLock = true) } answers {
            assertEquals(listOf("DISPATCH_REDIS_QUEUE_LOCK_ENV_consumer_env"), secondArg<List<String>>())
            queueLockHeld = false
            1L
        }
        every { dispatch.checkRunning(any(), any(), any(), any(), any(), any()) } returns true
        every { dao.fetchProjectDataCount(any(), any(), any(), any(), any()) } returns 0L
        every { envQueue.initEnvContext(any()) } returns EnvQueueContext(1L, emptyList())
        every { printer.addLines(any(), any(), any(), any()) } answers {
            assertFalse(queueLockHeld, "log RPC must run after the environment queue lock is released")
            batches.add(firstArg<String>() to secondArg<List<LogMessage>>())
        }
        every { envQueue.inEnvQueue(any(), any()) } answers {
            assertTrue(queueLockHeld)
            val data = secondArg<QueueDataContext>().data
            DispatchLogBuffer.withBuffer(data.buildId, printer) {
                repeat(30) { common.logDebug(data, "node $it") }
            }
            assertTrue(batches.isEmpty(), "nested per-build buffers must not flush while consuming the queue")
        }
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(I18nUtil)
    }

    private fun record(id: Long, buildId: String, vmSeqId: String = "1"): ThirdPartyAgentQueueSqlData {
        val type = ThirdPartyAgentEnvDispatchType("env", null, "owner", dockerInfo = null, reusedInfo = null)
        val message = DispatchMessage("id", "key", "gateway", event = PipelineAgentStartupEvent(
            source = "test", projectId = "consumer", pipelineId = "pipeline", pipelineName = "pipeline",
            userId = "test", buildId = buildId, buildNo = 1, vmSeqId = vmSeqId, taskName = "job", os = "LINUX",
            vmNames = "vm", dispatchType = type, containerId = vmSeqId, containerHashId = "container-$vmSeqId",
            executeCount = 2, jobId = "job-$vmSeqId"
        ))
        return ThirdPartyAgentQueueSqlData(id, ThirdPartyAgentDispatchData(message, type), LocalDateTime.now(), 1)
    }

    @Test
    fun wholeEventFlushesAfterUnlockAndKeepsBuildsAndJobsSeparate() {
        every { dao.fetchProjectData(any(), any(), any(), any(), any()) } returns listOf(
            record(1L, "build-a"), record(2L, "build-b"), record(3L, "build-a", "2")
        )

        queue.doQueue(event)

        assertEquals(listOf("build-a", "build-b"), batches.map { it.first })
        assertEquals(listOf(62, 31), batches.map { it.second.size })
        val aMessages = batches.first().second
        assertTrue(aMessages.take(31).all { it.jobId == "job-1" && it.containerHashId == "container-1" })
        assertTrue(aMessages.drop(31).all { it.jobId == "job-2" && it.containerHashId == "container-2" })
        assertTrue(aMessages.all { it.executeCount == 2 })
        assertEquals("##[debug]node 0", aMessages[1].message)
        assertEquals(VMUtils.genStartVMTaskId("2"), aMessages.last().tag)
        verify(exactly = 3) { dao.delete(any(), any()) }
        assertFalse(DispatchLogBuffer.append("build-a", "1", null, 1, null, "outside"))
    }

    @Test
    fun retryAndFailureLogsAreAlsoFlushedAfterUnlock() {
        every { dao.fetchProjectData(any(), any(), any(), any(), any()) } returns
            listOf(record(1L, "retry"), record(2L, "failure"))
        every { envQueue.inEnvQueue(any(), any()) } answers {
            val data = secondArg<QueueDataContext>().data
            common.logWarn(data, "waiting")
            if (data.buildId == "retry") {
                throw DispatchRetryMQException(ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR, "busy")
            }
            throw IllegalStateException("broken")
        }

        queue.doQueue(event)

        assertEquals(listOf("retry", "failure"), batches.map { it.first })
        assertEquals(Ansi().bold().fgYellow().a("waiting").reset().toString(), batches[0].second.last().message)
        val error = batches[1].second.last()
        assertTrue(error.message.contains(Ansi().bold().fgRed().toString()))
        assertTrue(error.message.contains("broken"))
        assertEquals(VMUtils.genStartVMTaskId("1"), error.stepId)
        verify(exactly = 1) { dao.addRetryTimeByIds(any(), setOf(1L)) }
        verify(exactly = 0) { dao.delete(any(), 1L) }
        verify(exactly = 1) { dao.delete(any(), 2L) }
        verify(exactly = 1) { dispatch.onFailure(any(), any(), any(), any(), any(), any()) }
        assertFalse(DispatchLogBuffer.append("failure", "1", null, 1, null, "outside"))
    }

    @Test
    fun logFailureDoesNotRequeueSuccessOrDropOtherBuilds() {
        every { dao.fetchProjectData(any(), any(), any(), any(), any()) } returns
            listOf(record(1L, "broken-log"), record(2L, "healthy-log"))
        every { printer.addLines("broken-log", any(), any(), any()) } answers {
            assertFalse(queueLockHeld)
            throw IllegalStateException("log unavailable")
        }

        queue.doQueue(event)

        assertEquals(listOf("healthy-log"), batches.map { it.first })
        verify(exactly = 2) { dao.delete(any(), any()) }
        verify(exactly = 1) { dao.addRetryTimeByIds(any(), emptySet()) }
        verify(exactly = 0) { dispatch.onFailure(any(), any(), any(), any(), any(), any()) }
        assertFalse(DispatchLogBuffer.append("healthy-log", "1", null, 1, null, "outside"))
    }

    @Test
    fun logsAreFlushedBeforeReacquiringLockForNextEvent() {
        every { dao.fetchProjectData(any(), any(), any(), any(), any()) } returns listOf(record(1L, "build"))
        every { dao.fetchProjectDataCount(any(), any(), any(), any(), any()) } returns 1L
        every { redis.setNxEx(any(), any(), any(), true) } answers {
            assertFalse(queueLockHeld)
            assertEquals(listOf("build"), batches.map { it.first })
            queueLockHeld = true
            true
        }

        queue.doQueue(event)

        verify(exactly = 1) { redis.setNxEx(any(), any(), any(), true) }
    }

    @Test
    fun multiBuildBufferChunksEachBuildAndSurvivesException() {
        queueLockHeld = false
        val failure = IllegalStateException("dispatch failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            DispatchLogBuffer.withBuffer(null, printer) {
                repeat(405) {
                    DispatchLogBuffer.append("build-a", "1", null, 1, null, "a$it")
                    if (it < 3) DispatchLogBuffer.append("build-b", "2", null, 1, null, "b$it")
                }
                throw failure
            }
        }

        assertEquals(failure, actual)
        assertEquals(listOf("build-a", "build-a", "build-a", "build-b"), batches.map { it.first })
        assertEquals(listOf(200, 200, 5, 3), batches.map { it.second.size })
        assertEquals((0..404).map { "a$it" }, batches.take(3).flatMap { it.second }.map { it.message })
        assertFalse(DispatchLogBuffer.append("build-a", "1", null, 1, null, "outside"))
    }

    @Test
    fun failedChunkDoesNotPreventLaterChunksFromBeingSent() {
        queueLockHeld = false
        every { printer.addLines("build", match { it.first().message == "0" }, any(), any()) } throws
            IllegalStateException("first batch unavailable")

        DispatchLogBuffer.withBuffer(null, printer) {
            repeat(201) { DispatchLogBuffer.append("build", "1", null, 1, null, it.toString()) }
        }

        assertEquals(listOf("200"), batches.single().second.map { it.message })
        verify(exactly = 2) { printer.addLines("build", any(), any(), any()) }
    }

    @Test
    fun singleBuildBufferStillRejectsUnrelatedBuilds() {
        queueLockHeld = false
        DispatchLogBuffer.withBuffer("build-a", printer) {
            assertFalse(DispatchLogBuffer.append("build-b", "1", null, 1, null, "unrelated"))
            assertTrue(DispatchLogBuffer.append("build-a", "1", null, 1, null, "expected"))
        }
        assertEquals(listOf("build-a"), batches.map { it.first })
    }
}
