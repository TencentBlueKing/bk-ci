package com.tencent.devops.dispatch

import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.dao.TPAQueueDao
import com.tencent.devops.dispatch.dao.ThirdPartyAgentQueueSqlData
import com.tencent.devops.dispatch.pojo.EnvQueueContext
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.TPAQueueEvent
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.tpaqueue.TPAEnvQueueService
import com.tencent.devops.dispatch.service.tpaqueue.TPAQueueService
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Characterization of review findings, not assertions that these behaviors are desirable. */
class TPAQueueReviewTest {
    private val redis = mockk<RedisOperation>(relaxed = true)
    private val printer = mockk<BuildLogPrinter>(relaxed = true)
    private val bridge = mockk<StreamBridge>(relaxed = true)
    private val dao = mockk<TPAQueueDao>(relaxed = true)
    private val envQueue = mockk<TPAEnvQueueService>(relaxed = true)
    private val dispatch = mockk<DispatchService>(relaxed = true)
    private val common = TPACommonUtil(mockk(relaxed = true), mockk(relaxed = true), printer)
    private val queue = TPAQueueService(mockk(), redis, bridge, common, dao, envQueue, dispatch, mockk(), printer)
    private val locks = ConcurrentHashMap<String, String>()
    private val sent = mutableListOf<TPAQueueEvent>()
    private val pending = mutableListOf<ThirdPartyAgentQueueSqlData>()

    @BeforeEach
    fun setup() {
        mockkObject(I18nUtil)
        every { I18nUtil.getDefaultLocaleLanguage() } returns "zh_CN"
        every { I18nUtil.getCodeLanMessage(any(), any(), any(), any(), any(), any()) } answers { firstArg() }
        every { redis.getKeyByRedisName(any()) } answers { firstArg() }
        every { redis.setNxEx(any(), any(), any(), true) } answers {
            locks.putIfAbsent(firstArg(), secondArg()) == null
        }
        every { redis.execute<Long>(any(), any(), *anyVararg(), isRedisLock = true) } answers {
            if (locks.remove(secondArg<List<String>>().single(), arg<Array<Any?>>(2).single())) 1L else 0L
        }
        every { bridge.send(any<String>(), any<Any>()) } answers {
            sent.add((secondArg<Any>() as Message<*>).payload as TPAQueueEvent)
            true
        }
        every { dispatch.checkRunning(any(), any(), any(), any(), any(), any()) } returns true
        every { envQueue.initEnvContext(any()) } returns EnvQueueContext(1L, emptyList())
        every { dao.fetchProjectData(any(), any(), any(), any(), any()) } answers {
            pending.filter { it.data.pipelineId == arg<String>(2) }
        }
        every { dao.fetchProjectDataCount(any(), any(), any(), any(), any()) } answers {
            pending.count { it.data.pipelineId == arg<String>(2) }.toLong()
        }
        every { dao.delete(any(), any()) } answers { pending.removeIf { it.recordId == secondArg<Long>() }; Unit }
    }

    @AfterEach
    fun cleanup() { unmockkObject(I18nUtil) }

    private fun data(pipeline: String): ThirdPartyAgentDispatchData {
        val type = ThirdPartyAgentEnvDispatchType("env", null, "owner", dockerInfo = null, reusedInfo = null)
        return ThirdPartyAgentDispatchData(DispatchMessage("id", "key", "gateway", event = PipelineAgentStartupEvent(
            source = "test", projectId = "consumer", pipelineId = pipeline, pipelineName = pipeline,
            userId = "test", buildId = "build-$pipeline", buildNo = 1, vmSeqId = "1", taskName = "job", os = "LINUX",
            vmNames = "vm", dispatchType = type, containerId = "1", containerHashId = "container", executeCount = 1,
            jobId = "job"
        )), type)
    }

    @Test
    fun reproduceOtherPipelinePendingWithoutAnEventAfterSharedEnvironmentLock() {
        val a = data("pipeline-a")
        val b = data("pipeline-b")
        pending.add(ThirdPartyAgentQueueSqlData(1L, a, LocalDateTime.now(), 1))
        pending.add(ThirdPartyAgentQueueSqlData(2L, b, LocalDateTime.now(), 1))
        queue.queue(a)
        queue.queue(b)
        assertEquals(listOf("pipeline-a"), sent.map { it.pipelineId })

        queue.doQueue(sent.single())

        assertTrue(locks.isEmpty())
        assertEquals(listOf("pipeline-b"), pending.map { it.data.pipelineId })
        assertEquals(listOf("pipeline-a"), sent.map { it.pipelineId }, "No event wakes pipeline-b")
    }

    @Test
    fun reproduceMqExceptionSwallowedAndSevenDayQueueLockRetained() {
        every { bridge.send(any<String>(), any<Any>()) } throws IllegalStateException("broker unavailable")
        queue.queue(data("pipeline"))
        assertEquals(1, locks.size, "sendTo swallows failure; dispatch does not release its queue lock")
        assertTrue(sent.isEmpty())
    }

    @Test
    fun reproduceMqFalseReturnRetainingQueueLock() {
        every { bridge.send(any<String>(), any<Any>()) } returns false
        queue.queue(data("pipeline"))
        assertEquals(1, locks.size, "sendTo ignores false; no message exists to release the lock")
        assertTrue(sent.isEmpty())
    }

    @Test
    fun reproduceSlowLogBlocksNextEventDespiteQueueLockBeingReleased() {
        val d = data("pipeline")
        pending.add(ThirdPartyAgentQueueSqlData(1L, d, LocalDateTime.now(), 1))
        queue.queue(d)
        val event = sent.single()
        every { envQueue.inEnvQueue(any(), any()) } answers {
            common.logDebug(secondArg<QueueDataContext>().data, "node status")
            throw com.tencent.devops.dispatch.exception.DispatchRetryMQException(
                com.tencent.devops.dispatch.exception.ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR, "busy"
            )
        }
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { printer.addLines(any(), any(), any(), any()) } answers {
            entered.countDown()
            assertTrue(release.await(10, TimeUnit.SECONDS))
        }
        val executor = Executors.newSingleThreadExecutor()
        try {
            val task = executor.submit { queue.doQueue(event) }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            assertTrue(locks.isEmpty())
            assertEquals(1, sent.size, "Next round not sent while log RPC is blocked")
            assertFalse(task.isDone)
            release.countDown()
            task.get(10, TimeUnit.SECONDS)
            assertEquals(2, sent.size)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }
}
