package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.pojo.EnvQueueContext
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.service.tpaqueue.TPAEnvQueueService
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.service.ThirdPartyDispatchService
import com.tencent.devops.dispatch.service.tpaqueue.TPASingleQueueService
import com.tencent.devops.dispatch.utils.DispatchLogBuffer
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor.TryAgentResult
import com.tencent.devops.dispatch.utils.ThirdPartyAgentJobEnvLock
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.EnabledStrategiesWithTags
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

class EnvDispatchGrayTest {
    @BeforeEach
    fun mockLocale() {
        mockkObject(I18nUtil)
        every { I18nUtil.getDefaultLocaleLanguage() } returns "zh_CN"
        every {
            I18nUtil.getCodeLanMessage(any(), any(), any(), any(), any(), any())
        } answers { firstArg() }
    }

    @AfterEach
    fun restoreLocale() {
        unmockkObject(I18nUtil)
    }

    private val locks = ConcurrentHashMap<String, String>()
    private val acquired = Collections.synchronizedList(mutableListOf<String>())
    private val redis = mockk<RedisOperation>(relaxed = true)
    private val printer = mockk<BuildLogPrinter>(relaxed = true)
    private val agents = mockk<ThirdPartyAgentService>(relaxed = true)
    private val single = mockk<TPASingleQueueService>(relaxed = true)
    private val service = ThirdPartyDispatchService(
        mockk(), redis, printer, mockk(relaxed = true), agents, mockk(), single
    )
    private val agent = ThirdPartyAgent(
        agentId = "agent", projectId = "owner", nodeId = null, status = AgentStatus.IMPORT_OK,
        hostname = "host", os = "LINUX", ip = "127.0.0.1", secretKey = "test", createUser = "test",
        createTime = 0L, parallelTaskCount = 4, dockerParallelTaskCount = 2, masterVersion = null
    )
    private val strategy = DispatchStrategyConfig.buildDefaults("owner", 1L, "test")
        .first().copy(scope = StrategyScope.ALL, nodeRule = NodeRule.IDLE)

    init {
        every { redis.getKeyByRedisName(any()) } answers { firstArg() }
        every { redis.setNxEx(any(), any(), any(), true) } answers {
            val key = firstArg<String>()
            val value = secondArg<String>()
            val success = locks.putIfAbsent(key, value) == null
            if (success) acquired.add(key)
            success
        }
        every { redis.execute<Long>(any(), any(), *anyVararg(), isRedisLock = true) } answers {
            val key = secondArg<List<String>>().single()
            val token = arg<Array<Any?>>(2).single() as String
            if (locks.remove(key, token)) 1L else 0L
        }
        every { agents.getEnvStrategiesWithTags(any(), any(), any()) } returns EnabledStrategiesWithTags(
            listOf(strategy), emptyMap(), emptyMap()
        )
        every { agents.countRunningAndQueueByAgents(any()) } returns emptyMap()
        every { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) } answers {
            assertTrue(locks.keys.any { it.startsWith("DISPATCH_REDIS_LOCK_JOB_ENV_") })
            TryAgentResult.SUCCESS
        }
    }

    private fun message(jobId: String? = "job", singleLimit: Int? = null, allLimit: Int? = null): DispatchMessage {
        val type = ThirdPartyAgentEnvDispatchType("env", null, "owner", dockerInfo = null, reusedInfo = null)
        return DispatchMessage("id", "key", "gateway", event = PipelineAgentStartupEvent(
            source = "test", projectId = "consumer", pipelineId = "pipeline", pipelineName = "pipeline",
            userId = "test", buildId = "build", buildNo = 1, vmSeqId = "1", taskName = "job", os = "LINUX",
            vmNames = "vm", dispatchType = type, containerId = "1", containerHashId = "container", executeCount = 1,
            jobId = jobId, singleNodeConcurrency = singleLimit, allNodeConcurrency = allLimit
        ))
    }

    private fun screen(message: DispatchMessage = message(), candidates: List<ThirdPartyAgent> = listOf(agent)) {
        val method = ThirdPartyDispatchService::class.java.getDeclaredMethod(
            "screenEnvNode", DispatchMessage::class.java, ThirdPartyAgentEnvDispatchType::class.java,
            List::class.java, Long::class.javaPrimitiveType
        ).apply { isAccessible = true }
        try {
            method.invoke(service, message, message.event.dispatchType, candidates, 1L)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    @Test
    fun onlyExplicitTrueEnablesGray() {
        for (value in listOf(null, "", "false", "1", "TRUE")) {
            every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns value
            assertFalse(service.useFineGrainedEnvLock("consumer"))
        }
        every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns "true"
        assertTrue(service.useFineGrainedEnvLock("consumer"))
        every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } throws
            IllegalStateException("redis unavailable")
        assertFalse(service.useFineGrainedEnvLock("consumer"))
    }

    @Test
    fun defaultUsesEnvironmentThenJobLockAndReleasesBeforeLogs() {
        every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns null
        every { printer.addLines(any(), any(), any(), any()) } answers {
            assertTrue(locks.isEmpty())
        }
        screen()
        assertEquals("DISPATCH_REDIS_LOCK_ENV_owner_1", acquired[0])
        assertEquals(ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L), acquired[1])
        assertTrue(locks.isEmpty())
        verify(exactly = 1) { printer.addLines("build", any(), any(), any()) }
    }

    @Test
    fun grayUsesOnlyJobLockAndReadsSnapshotBeforeLock() {
        every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns "true"
        every { agents.countRunningAndQueueByAgents(any()) } answers {
            assertTrue(locks.isEmpty())
            emptyMap()
        }
        screen()
        assertEquals(listOf(ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L)), acquired)
        assertTrue(locks.isEmpty())
    }

    @Test
    fun bothModesRecheckQuotaUnderSameJobLock() {
        for (gray in listOf("true", "false")) {
            every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns gray
            var count = 0
            every { agents.countProjectJobRunningAndQueueAll(any(), any(), any(), any()) } answers {
                count++
                if (count == 1) {
                    assertTrue(locks.isEmpty())
                    0L
                } else {
                    assertTrue(locks.containsKey(ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L)))
                    1L
                }
            }
            assertThrows(DispatchRetryMQException::class.java) { screen(message(allLimit = 1)) }
            assertTrue(locks.isEmpty())
        }
        verify(exactly = 0) { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun grayToggleCannotBypassInFlightJobQuotaLock() {
        val key = ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L)
        locks[key] = "in-flight"
        for (gray in listOf("true", "false")) {
            every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns gray
            assertThrows(DispatchRetryMQException::class.java) { screen() }
            assertEquals(mapOf(key to "in-flight"), locks)
        }
        verify(exactly = 0) { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun jobKeysAreIsolatedAndDoNotCollideOnSeparators() {
        val key = ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L)
        assertNotEquals(key, ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job2", 1L))
        assertNotEquals(key, ThirdPartyAgentJobEnvLock.key("consumer", "pipeline2", "job", 1L))
        assertNotEquals(key, ThirdPartyAgentJobEnvLock.key("other", "pipeline", "job", 1L))
        assertNotEquals(key, ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 2L))
        assertNotEquals(
            ThirdPartyAgentJobEnvLock.key("a_b", "c", "job", 1L),
            ThirdPartyAgentJobEnvLock.key("a", "b_c", "job", 1L)
        )
    }

    @Test
    fun sharedStrategyUsesOwnerButHistoryUsesConsumerAndIsLazy() {
        screen()
        verify(exactly = 1) { agents.getEnvStrategiesWithTags("owner", 1L, emptySet()) }
        verify(exactly = 0) { agents.getPreBuildAgentIds(any(), any(), any(), any()) }
        every { agents.getEnvStrategiesWithTags(any(), any(), any()) } returns EnabledStrategiesWithTags(
            listOf(strategy.copy(scope = StrategyScope.PRE_BUILD)), emptyMap(), emptyMap()
        )
        every { agents.getPreBuildAgentIds("consumer", "pipeline", "1", 1) } returns listOf("agent")
        screen()
        verify(exactly = 1) { agents.getPreBuildAgentIds("consumer", "pipeline", "1", 1) }
    }

    @Test
    fun zeroSingleLimitQueuesInsteadOfThrowingNullPointer() {
        every { agents.countAgentsJobRunningAndQueueAll(any(), any(), any(), any(), any()) } returns emptyMap()
        assertThrows(DispatchRetryMQException::class.java) { screen(message(singleLimit = 0)) }
        assertTrue(acquired.isEmpty())
    }

    @Test
    fun quotaKeepsOriginalAgentSetDuringLockedRecheck() {
        val candidates = listOf(agent, agent.copy(agentId = "other"))
        every { agents.countAgentsJobRunningAndQueueAll(any(), any(), any(), any(), any()) } returns mapOf("other" to 1)
        screen(message(singleLimit = 1), candidates)
        verify(exactly = 2) {
            agents.countAgentsJobRunningAndQueueAll("consumer", "pipeline", 1L, "job", setOf("agent", "other"))
        }
    }

    @Test
    fun loadChangesAreRecheckedUnderAgentLockAndDoNotQueue() {
        val data = ThirdPartyAgentDispatchData(
            message(),
            message().event.dispatchType as ThirdPartyAgentEnvDispatchType
        )
        val queue = TPASingleQueueService(redis, mockk(), mockk<Client>(), mockk(relaxed = true), mockk(), agents)
        every { agents.countRunningAndQueueByAgents(setOf("agent")) } answers {
            assertTrue(locks.containsKey("DISPATCH_REDIS_LOCK_AGENT_consumer_agent"))
            mapOf("agent" to Pair(4, 0))
        }
        var refreshed: Pair<Int, Int>? = null
        assertEquals(TryAgentResult.LOAD_CHANGED, queue.tryAgentWithRule(data, agent, 1L, NodeRule.AVAILABLE) {
            refreshed = it
        })
        assertEquals(Pair(4, 0), refreshed)
        verify(exactly = 0) { agents.queueBuild(any(), any(), any(), any()) }
        assertTrue(locks.isEmpty())
    }

    @Test
    fun loadChangedCanFallbackToAvailableButRealRejectionIsDeduplicated() {
        val running = mutableMapOf("agent" to 0)
        val input = DispatchStrategyExecutor.StrategyInput(
            listOf(agent), emptySet(), running, emptyMap(), emptyMap(), emptyMap(), false
        )
        val seen = mutableListOf<NodeRule?>()
        val result = DispatchStrategyExecutor(input).executeWithRule(
            listOf(strategy, strategy.copy(nodeRule = NodeRule.AVAILABLE))
        ) { _, rule ->
            seen.add(rule)
            if (rule == NodeRule.IDLE) {
                running["agent"] = 1
                TryAgentResult.LOAD_CHANGED
            } else TryAgentResult.SUCCESS
        }
        assertEquals(agent, result)
        assertEquals(listOf(NodeRule.IDLE, NodeRule.AVAILABLE), seen)
        var attempts = 0
        DispatchStrategyExecutor(input).executeWithRule(
            listOf(strategy.copy(nodeRule = NodeRule.AVAILABLE), strategy.copy(nodeRule = null))
        ) { _, _ -> attempts++; TryAgentResult.REJECTED }
        assertEquals(1, attempts)
    }

    @Test
    fun nullRuleAndDockerCapacityKeepExistingSemantics() {
        assertTrue(DispatchStrategyExecutor.matchesNodeRule(agent, null, false, 99, 99))
        assertFalse(DispatchStrategyExecutor.matchesNodeRule(agent, NodeRule.AVAILABLE, true, 1, 2))
        assertTrue(DispatchStrategyExecutor.matchesNodeRule(agent, NodeRule.IDLE, true, 99, 0))
        assertTrue(DispatchStrategyExecutor.matchesNodeRule(
            agent.copy(dockerParallelTaskCount = 0), NodeRule.AVAILABLE, true, 0, 99
        ))
    }

    @Test
    fun logBufferFlushesAfterExceptionPreservesFormatAndClearsThreadLocal() {
        val batches = mutableListOf<List<LogMessage>>()
        every { printer.addLines(any(), any(), any(), any()) } answers { batches.add(secondArg()) }
        val before = System.currentTimeMillis()
        assertThrows(IllegalStateException::class.java) {
            DispatchLogBuffer.withBuffer("build", printer) {
                assertTrue(DispatchLogBuffer.append("build", "1", "container", 2, "job", "first"))
                DispatchLogBuffer.withBuffer("build", printer) {
                    DispatchLogBuffer.append("build", "1", "container", 2, "job", "a\nb", LogType.DEBUG)
                }
                assertTrue(batches.isEmpty())
                throw IllegalStateException("dispatch failed")
            }
        }
        assertEquals(1, batches.size)
        val messages = batches.single()
        assertEquals(listOf("first", "##[debug]a\n##[debug]b"), messages.map { it.message })
        assertTrue(messages.all { it.timestamp >= before && it.timestamp <= System.currentTimeMillis() })
        assertEquals(LogType.DEBUG, messages.last().logType)
        assertEquals(2, messages.last().executeCount)
        assertFalse(DispatchLogBuffer.append("build", "1", null, 1, null, "outside"))
    }

    @Test
    fun logFailureCannotOverwriteSuccessfulDispatch() {
        every { printer.addLines(any(), any(), any(), any()) } throws IllegalStateException("log unavailable")
        val result = DispatchLogBuffer.withBuffer("build", printer) {
            DispatchLogBuffer.append("build", "1", null, null, null, "success")
            "queued"
        }
        assertEquals("queued", result)
        assertFalse(DispatchLogBuffer.append("build", "1", null, null, null, "outside"))
    }

    @Test
    fun logBufferBatchesLargeEnvironmentLogs() {
        val sizes = mutableListOf<Int>()
        every { printer.addLines(any(), any(), any(), any()) } answers { sizes.add(secondArg<List<LogMessage>>().size) }
        DispatchLogBuffer.withBuffer("build", printer) {
            repeat(405) { DispatchLogBuffer.append("build", "1", null, 1, null, "agent $it") }
        }
        assertEquals(listOf(200, 200, 5), sizes)
    }

    @Test
    fun differentJobsCanPickInParallelInGrayMode() {
        every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns "true"
        val entered = CountDownLatch(2)
        every { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) } answers {
            entered.countDown()
            assertTrue(entered.await(5, TimeUnit.SECONDS), "different Jobs should not share an environment lock")
            TryAgentResult.SUCCESS
        }
        val workers = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map { index -> workers.submit { screen(message(jobId = "job$index")) } }
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
            assertFalse(acquired.any { it.startsWith("DISPATCH_REDIS_LOCK_ENV_") })
            assertEquals(2, acquired.size)
            assertTrue(locks.isEmpty())
        } finally {
            workers.shutdownNow()
        }
    }

    @Test
    fun actualQueueWriteAndCapacityCheckShareAgentLock() {
        val msg = message(jobId = null)
        val data = ThirdPartyAgentDispatchData(msg, msg.event.dispatchType as ThirdPartyAgentEnvDispatchType)
        val queue = TPASingleQueueService(
            redis, mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), agents
        )
        val key = "DISPATCH_REDIS_LOCK_AGENT_consumer_agent"
        every { agents.countRunningAndQueueByAgents(setOf("agent")) } answers {
            assertTrue(locks.containsKey(key))
            mapOf("agent" to Pair(1, 0))
        }
        every { agents.queueBuild(any(), any(), any(), any()) } answers { assertTrue(locks.containsKey(key)) }
        assertEquals(TryAgentResult.SUCCESS, queue.tryAgentWithRule(data, agent, 1L, NodeRule.AVAILABLE, true))
        verify(exactly = 1) { agents.queueBuild(any(), any(), any(), any()) }
        assertTrue(locks.isEmpty())
    }

    @Test
    fun existingQueueUsesOwnerStrategyAndSameJobLock() {
        val msg = message()
        val data = ThirdPartyAgentDispatchData(msg, msg.event.dispatchType as ThirdPartyAgentEnvDispatchType)
        val queue = TPAEnvQueueService(mockk(), mockk(relaxed = true), agents, single, redis)
        queue.inEnvQueue(EnvQueueContext(1L, listOf(agent)), QueueDataContext(data, 1))
        verify(exactly = 1) { agents.getEnvStrategiesWithTags("owner", 1L, emptySet()) }
        assertEquals(listOf(ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L)), acquired)
        assertTrue(locks.isEmpty())
    }

    @Test
    fun existingQueueCannotUseOldQuotaCacheToAdmitBuild() {
        val msg = message(allLimit = 1)
        val data = ThirdPartyAgentDispatchData(msg, msg.event.dispatchType as ThirdPartyAgentEnvDispatchType)
        val context = EnvQueueContext(1L, listOf(agent))
        context.setProjectJobRunningAndQueueAllCount("job", 0L)
        every { agents.countProjectJobRunningAndQueueAll(any(), any(), any(), any()) } answers {
            assertTrue(locks.containsKey(ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L)))
            1L
        }
        val queue = TPAEnvQueueService(mockk(), mockk(relaxed = true), agents, single, redis)
        assertThrows(DispatchRetryMQException::class.java) { queue.inEnvQueue(context, QueueDataContext(data, 1)) }
        verify(exactly = 0) { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) }
        assertTrue(locks.isEmpty())
    }
}
