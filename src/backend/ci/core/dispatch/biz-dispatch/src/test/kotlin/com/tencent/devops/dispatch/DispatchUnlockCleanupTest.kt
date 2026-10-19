package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.constants.BK_SCHEDULING_SELECTED_AGENT
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.pojo.EnvQueueContext
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.service.ThirdPartyDispatchService
import com.tencent.devops.dispatch.service.tpaqueue.TPAEnvQueueService
import com.tencent.devops.dispatch.service.tpaqueue.TPASingleQueueService
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor.TryAgentResult
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.ThirdPartyAgentJobEnvLock
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.EnabledStrategiesWithTags
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cloud.stream.function.StreamBridge
import java.lang.reflect.InvocationTargetException

class DispatchUnlockCleanupTest {
    private val agentKey = "DISPATCH_REDIS_LOCK_AGENT_consumer_agent"
    private val jobKey = ThirdPartyAgentJobEnvLock.key("consumer", "pipeline", "job", 1L)
    private val envKey = "DISPATCH_REDIS_LOCK_ENV_owner_1"
    private val locks = mutableMapOf<String, String>()
    private val failedKeys = mutableSetOf<String>()
    private val trace = mutableListOf<String>()
    private val redis = mockk<RedisOperation>(relaxed = true)
    private val printer = mockk<BuildLogPrinter>(relaxed = true)
    private val common = mockk<TPACommonUtil>(relaxed = true)
    private val agents = mockk<ThirdPartyAgentService>(relaxed = true)
    private val builds = mockk<ThirdPartyAgentBuildRedisUtils>(relaxed = true)
    private val client = mockk<Client>(relaxed = true)
    private val details = mockk<ServiceBuildResource>(relaxed = true)
    private val bridge = mockk<StreamBridge>(relaxed = true)
    private val single = spyk(TPASingleQueueService(redis, bridge, client, common, builds, agents))
    private val service = ThirdPartyDispatchService(client, redis, printer, common, agents, mockk(), single)
    private val agent = ThirdPartyAgent(
        agentId = "agent", projectId = "owner", nodeId = null, status = AgentStatus.IMPORT_OK,
        hostname = "host", os = "LINUX", ip = "127.0.0.1", secretKey = "test", createUser = "test",
        createTime = 0L, parallelTaskCount = 4, dockerParallelTaskCount = 2, masterVersion = null
    )
    private val type = ThirdPartyAgentEnvDispatchType("env", null, "owner", dockerInfo = null, reusedInfo = null)
    private val message = DispatchMessage("id", "key", "gateway", event = PipelineAgentStartupEvent(
        source = "test", projectId = "consumer", pipelineId = "pipeline", pipelineName = "pipeline",
        userId = "test", buildId = "build", buildNo = 1, vmSeqId = "1", taskName = "job", os = "LINUX",
        vmNames = "vm", dispatchType = type, containerId = "1", containerHashId = "container", executeCount = 1,
        jobId = "job", dispatchQueueStartTimeMilliSecond = 1L, jobTimeoutMinutes = 60
    ))
    private val data = ThirdPartyAgentDispatchData(message, type)

    @BeforeEach
    fun setUp() {
        mockkObject(I18nUtil)
        every { I18nUtil.getDefaultLocaleLanguage() } returns "zh_CN"
        every { I18nUtil.getCodeLanMessage(any(), any(), any(), any(), any(), any()) } answers { firstArg() }
        every { redis.getKeyByRedisName(any()) } answers { firstArg() }
        every { redis.setNxEx(any(), any(), any(), true) } answers {
            locks.putIfAbsent(firstArg(), secondArg()) == null
        }
        every { redis.execute<Long>(any(), any(), *anyVararg(), isRedisLock = true) } answers {
            val key = secondArg<List<String>>().single()
            trace.add("unlock:$key")
            if (key in failedKeys) throw IllegalStateException("unlock:$key")
            val token = arg<Array<Any?>>(2).single() as String
            if (locks.remove(key, token)) 1L else 0L
        }
        val strategy = DispatchStrategyConfig.buildDefaults("owner", 1L, "test")
            .first().copy(scope = StrategyScope.ALL, nodeRule = NodeRule.IDLE)
        every { agents.getEnvStrategiesWithTags(any(), any(), any()) } returns EnabledStrategiesWithTags(
            listOf(strategy), emptyMap(), emptyMap()
        )
        every { agents.countRunningAndQueueByAgents(any()) } returns emptyMap()
        every { agents.queueBuild(any(), any(), any(), any()) } answers {
            assertTrue(locks.containsKey(agentKey))
            trace.add("sql")
        }
        every { builds.setThirdPartyBuild(any(), any()) } answers {
            assertTrue(locks.containsKey(agentKey))
            trace.add("redis")
        }
        every { client.get(ServiceBuildResource::class) } returns details
        every { bridge.send(any<String>(), any()) } returns true
        every { single.afterAgentInQueue(any(), any()) } answers {
            trace.add("post")
            callOriginal()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(I18nUtil)
    }

    private fun screen(gray: Boolean = false, startup: PipelineAgentStartupEvent = message.event) {
        every { redis.hget(ThirdPartyDispatchService.DISPATCH_ENV_FINE_LOCK_PROJECTS, "consumer") } returns
            gray.toString()
        val method = ThirdPartyDispatchService::class.java.getDeclaredMethod(
            "screenEnvNode", DispatchMessage::class.java, ThirdPartyAgentEnvDispatchType::class.java,
            List::class.java, Long::class.javaPrimitiveType
        ).apply { isAccessible = true }
        try {
            method.invoke(service, DispatchMessage("id", "key", "gateway", event = startup), type, listOf(agent), 1L)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun assertPostedOnce(unlockedBeforePost: Set<String>) {
        verify(exactly = 1) { agents.queueBuild(any(), any(), any(), any()) }
        verify(exactly = 1) { builds.setThirdPartyBuild(any(), any()) }
        verify(exactly = 1) { single.afterAgentInQueue(any(), agent) }
        verify(exactly = 1) { details.saveBuildVmInfo("consumer", "pipeline", "build", "1", any()) }
        verify(exactly = 1) { common.logWithAgentUrl(any(), BK_SCHEDULING_SELECTED_AGENT, any(), any(), any()) }
        verify(exactly = 1) { bridge.send(any<String>(), any()) }
        val post = trace.indexOf("post")
        assertTrue(trace.indexOf("sql") < trace.indexOf("redis"))
        unlockedBeforePost.forEach { key ->
            assertTrue(trace.indexOf("redis") < trace.indexOf("unlock:$key"))
            assertTrue(trace.lastIndexOf("unlock:$key") < post)
        }
        failedKeys.forEach { key -> assertEquals(2, trace.count { it == "unlock:$key" }) }
        assertEquals(failedKeys, locks.keys)
    }

    private fun assertNotPosted() {
        verify(exactly = 0) { single.afterAgentInQueue(any(), any()) }
        verify(exactly = 0) { details.saveBuildVmInfo(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { bridge.send(any<String>(), any()) }
        verify(exactly = 0) { common.updateQueueTime(any<PipelineAgentStartupEvent>(), any(), any()) }
    }

    private fun successfulScreen(gray: Boolean, vararg failures: String) {
        failedKeys.addAll(failures)
        screen(gray)
        assertPostedOnce(if (gray) setOf(agentKey, jobKey) else setOf(agentKey, jobKey, envKey))
        verify(exactly = 1) { common.updateQueueTime(message.event, 1L, any()) }
    }

    @Test
    fun defaultAgentUnlockFailureStillPosts() = successfulScreen(false, agentKey)

    @Test
    fun defaultJobUnlockFailureStillPosts() = successfulScreen(false, jobKey)

    @Test
    fun defaultEnvironmentUnlockFailureStillPosts() = successfulScreen(false, envKey)

    @Test
    fun allUnlockFailuresStillPostExactlyOnce() = successfulScreen(false, agentKey, jobKey, envKey)

    @Test
    fun fineGrainedAgentUnlockFailureStillPosts() = successfulScreen(true, agentKey)

    @Test
    fun fineGrainedJobUnlockFailureStillPosts() = successfulScreen(true, jobKey)

    @Test
    fun normalDispatchPostsOnlyAfterAllLocksRelease() = successfulScreen(false)

    @Test
    fun directAgentUnlockFailureStillPosts() {
        failedKeys.add(agentKey)
        assertTrue(single.doAgentInQueue(data, agent, null))
        assertPostedOnce(setOf(agentKey))
    }

    @Test
    fun deferredAgentUnlockFailureReturnsSuccessWithoutEarlyPost() {
        failedKeys.add(agentKey)
        assertEquals(TryAgentResult.SUCCESS, single.tryAgentWithRule(data, agent, 1L, NodeRule.IDLE, true))
        assertNotPosted()
        single.afterAgentInQueue(data, agent)
        assertPostedOnce(setOf(agentKey))
    }

    private fun successfulNewQueue(vararg failures: String) {
        failedKeys.addAll(failures)
        val context = EnvQueueContext(1L, listOf(agent))
        TPAEnvQueueService(client, common, agents, single, redis).inEnvQueue(context, QueueDataContext(data, 1))
        assertPostedOnce(setOf(agentKey))
        assertFalse(context.agentRunningCnt.containsKey(agent.agentId))
        assertFalse(context.dockerRunningCnt.containsKey(agent.agentId))
    }

    @Test
    fun newQueueAgentUnlockFailureStillPostsAndClearsLoadCache() = successfulNewQueue(agentKey)

    @Test
    fun newQueueJobUnlockFailureKeepsSuccessAndDoesNotRepeatPost() = successfulNewQueue(jobKey)

    @Test
    fun queueWriteFailureKeepsOriginalExceptionEvenIfAllUnlocksFail() {
        failedKeys.addAll(setOf(agentKey, jobKey, envKey))
        val failure = IllegalArgumentException("sql write failed")
        every { agents.queueBuild(any(), any(), any(), any()) } throws failure
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) { screen() })
        assertEquals(3, failure.suppressed.size)
        verify(exactly = 0) { builds.setThirdPartyBuild(any(), any()) }
        assertNotPosted()
    }

    @Test
    fun redisReservationFailureIsNotTreatedAsQueued() {
        failedKeys.add(agentKey)
        val failure = IllegalArgumentException("redis reservation failed")
        every { builds.setThirdPartyBuild(any(), any()) } throws failure
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) { screen() })
        assertEquals(1, failure.suppressed.size)
        assertNotPosted()
    }

    @Test
    fun capacityRejectionWithUnlockFailureDoesNotQueueOrPost() {
        failedKeys.add(agentKey)
        every { agents.countRunningAndQueueByAgents(any()) } returns mapOf("agent" to Pair(4, 0))
        assertThrows(IllegalStateException::class.java) {
            single.tryAgentWithRule(data, agent, 1L, NodeRule.IDLE)
        }
        verify(exactly = 0) { agents.queueBuild(any(), any(), any(), any()) }
        assertNotPosted()
    }

    @Test
    fun quotaRejectionWithUnlockFailuresRemainsRetry() {
        failedKeys.addAll(setOf(jobKey, envKey))
        var reads = 0
        every { agents.countProjectJobRunningAndQueueAll(any(), any(), any(), any()) } answers {
            if (++reads == 1) 0L else 1L
        }
        val failure = assertThrows(DispatchRetryMQException::class.java) {
            screen(startup = message.event.copy(allNodeConcurrency = 1))
        }
        assertEquals(2, failure.suppressed.size)
        verify(exactly = 0) { agents.queueBuild(any(), any(), any(), any()) }
        assertNotPosted()
    }

    @Test
    fun busyAgentDoesNotQueueOrPost() {
        locks[agentKey] = "other-consumer"
        assertFalse(single.doAgentInQueue(data, agent, null))
        assertEquals("other-consumer", locks[agentKey])
        verify(exactly = 0) { agents.queueBuild(any(), any(), any(), any()) }
        assertNotPosted()
    }

    @Test
    fun busyJobDoesNotQueueOrPost() {
        locks[jobKey] = "other-consumer"
        assertThrows(DispatchRetryMQException::class.java) { screen() }
        assertEquals(mapOf(jobKey to "other-consumer"), locks)
        verify(exactly = 0) { agents.queueBuild(any(), any(), any(), any()) }
        assertNotPosted()
    }

    @Test
    fun detailFailureAfterUnlockFailureStillAttemptsLogAndMonitor() {
        failedKeys.addAll(setOf(agentKey, jobKey, envKey))
        every { details.saveBuildVmInfo(any(), any(), any(), any(), any()) } throws
            IllegalStateException("details unavailable")
        screen()
        assertPostedOnce(setOf(agentKey, jobKey, envKey))
    }
}
