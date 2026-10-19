package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.pojo.EnvQueueContext
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.service.tpaqueue.TPAEnvQueueService
import com.tencent.devops.dispatch.service.tpaqueue.TPASingleQueueService
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor.TryAgentResult
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.EnabledStrategiesWithTags
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
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

class TPAEnvQueuePerformanceTest {
    private val redis = mockk<RedisOperation>(relaxed = true)
    private val agents = mockk<ThirdPartyAgentService>(relaxed = true)
    private val single = mockk<TPASingleQueueService>(relaxed = true)
    private val common = mockk<TPACommonUtil>(relaxed = true)
    private val queue = TPAEnvQueueService(mockk(), common, agents, single, redis)
    private val agent = ThirdPartyAgent(
        agentId = "agent", projectId = "owner", nodeId = null, status = AgentStatus.IMPORT_OK,
        hostname = "host", os = "LINUX", ip = "127.0.0.1", secretKey = "test", createUser = "test",
        createTime = 0L, parallelTaskCount = 4, dockerParallelTaskCount = 2, masterVersion = null
    )

    @BeforeEach
    fun setup() {
        mockkObject(I18nUtil)
        every { I18nUtil.getDefaultLocaleLanguage() } returns "zh_CN"
        every { I18nUtil.getCodeLanMessage(any(), any(), any(), any(), any(), any()) } answers { firstArg() }
        every { redis.getKeyByRedisName(any()) } answers { firstArg() }
        every { redis.setNxEx(any(), any(), any(), true) } returns true
        every { redis.execute<Long>(any(), any(), *anyVararg(), isRedisLock = true) } returns 1L
        val strategy = DispatchStrategyConfig.buildDefaults("owner", 1L, "test").first()
            .copy(scope = StrategyScope.ALL, nodeRule = null)
        every { agents.getEnvStrategiesWithTags(any(), any(), any()) } returns EnabledStrategiesWithTags(
            listOf(strategy), emptyMap(), emptyMap()
        )
        every { agents.countRunningAndQueueByAgents(any()) } returns emptyMap()
        every { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) } returns TryAgentResult.REJECTED
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(I18nUtil)
    }

    private fun data(isDocker: Boolean = false): ThirdPartyAgentDispatchData {
        val docker = if (isDocker) ThirdPartyAgentDockerInfo("image", null, null, null, storeImage = null) else null
        val type = ThirdPartyAgentEnvDispatchType("env", null, "owner", dockerInfo = docker, reusedInfo = null)
        val message = DispatchMessage("id", "key", "gateway", event = PipelineAgentStartupEvent(
            source = "test", projectId = "consumer", pipelineId = "pipeline", pipelineName = "pipeline",
            userId = "test", buildId = "build", buildNo = 1, vmSeqId = "1", taskName = "job", os = "LINUX",
            vmNames = "vm", dispatchType = type, containerId = "1", containerHashId = "container", executeCount = 1,
            jobId = "job"
        ))
        return ThirdPartyAgentDispatchData(message, type)
    }

    private fun retry(context: EnvQueueContext, isDocker: Boolean = false) {
        assertThrows(DispatchRetryMQException::class.java) {
            queue.inEnvQueue(context, QueueDataContext(data(isDocker), 1))
        }
    }

    @Test
    fun thirtyDockerNodesUseOneBatchInsteadOfSixtyListQueries() {
        val candidates = (1..30).map { agent.copy(agentId = "agent$it") }
        val ids = candidates.map { it.agentId }.toSet()
        val context = EnvQueueContext(1L, candidates)
        every { agents.countRunningAndQueueByAgents(ids) } returns mapOf("agent1" to Pair(2, 1))

        retry(context, isDocker = true)

        verify(exactly = 1) { agents.countRunningAndQueueByAgents(ids) }
        verify(exactly = 0) { agents.getRunningBuilds(any()) }
        verify(exactly = 0) { agents.getDockerRunningBuilds(any()) }
        verify(exactly = 30) { common.logDebug(any(), match { it.contains(", Jobs:") }) }
        assertEquals(2, context.agentRunningCnt["agent1"])
        assertEquals(1, context.dockerRunningCnt["agent1"])
        assertEquals(0, context.agentRunningCnt["agent30"])
        assertEquals(0, context.dockerRunningCnt["agent30"])
    }

    @Test
    fun normalAndDockerJobsReuseBothCountsIncludingZero() {
        val context = EnvQueueContext(1L, listOf(agent))
        retry(context)
        retry(context, isDocker = true)

        verify(exactly = 1) { agents.countRunningAndQueueByAgents(setOf("agent")) }
        assertEquals(mapOf("agent" to 0), context.agentRunningCnt)
        assertEquals(mapOf("agent" to 0), context.dockerRunningCnt)
    }

    @Test
    fun partialCacheAndNewNodesAreFilledByOneBatch() {
        val context = EnvQueueContext(1L, listOf(agent, agent.copy(agentId = "partial")))
        context.agentRunningCnt.putAll(mapOf("agent" to 3, "partial" to 9))
        context.dockerRunningCnt["agent"] = 1
        context.agents = context.agents + agent.copy(agentId = "new")
        every { agents.countRunningAndQueueByAgents(setOf("partial", "new")) } returns
            mapOf("partial" to Pair(2, 1))

        retry(context, isDocker = true)

        verify(exactly = 1) { agents.countRunningAndQueueByAgents(any()) }
        verify(exactly = 1) { agents.countRunningAndQueueByAgents(setOf("partial", "new")) }
        assertEquals(mapOf("agent" to 3, "partial" to 2, "new" to 0), context.agentRunningCnt)
        assertEquals(mapOf("agent" to 1, "partial" to 1, "new" to 0), context.dockerRunningCnt)
    }

    @Test
    fun successfulReservationOnlyInvalidatesSelectedNodeForNextJob() {
        val context = EnvQueueContext(1L, listOf(agent, agent.copy(agentId = "other")))
        every { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) } returns TryAgentResult.SUCCESS

        queue.inEnvQueue(context, QueueDataContext(data(), 1))
        assertFalse(context.agentRunningCnt.containsKey("agent"))
        assertFalse(context.dockerRunningCnt.containsKey("agent"))
        assertEquals(0, context.agentRunningCnt["other"])
        queue.inEnvQueue(context, QueueDataContext(data(isDocker = true), 1))

        verify(exactly = 1) { agents.countRunningAndQueueByAgents(setOf("agent", "other")) }
        verify(exactly = 1) { agents.countRunningAndQueueByAgents(setOf("agent")) }
        verify(exactly = 2) { agents.countRunningAndQueueByAgents(any()) }
    }

    @Test
    fun failedBatchCannotCacheFalseZeroLoads() {
        val context = EnvQueueContext(1L, listOf(agent))
        every { agents.countRunningAndQueueByAgents(any()) } throws IllegalStateException("database unavailable")

        assertThrows(IllegalStateException::class.java) {
            queue.inEnvQueue(context, QueueDataContext(data(), 1))
        }
        assertTrue(context.agentRunningCnt.isEmpty())
        assertTrue(context.dockerRunningCnt.isEmpty())
        verify(exactly = 0) { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) }

        every { agents.countRunningAndQueueByAgents(any()) } returns emptyMap()
        retry(context)
        verify(exactly = 2) { agents.countRunningAndQueueByAgents(setOf("agent")) }
    }

    @Test
    fun loadChangedCallbackStillRefreshesTheSharedSnapshot() {
        val context = EnvQueueContext(1L, listOf(agent))
        every { single.tryAgentWithRule(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Pair<Int, Int>) -> Unit>(5).invoke(Pair(4, 2))
            TryAgentResult.LOAD_CHANGED
        }

        retry(context, isDocker = true)

        assertEquals(4, context.agentRunningCnt["agent"])
        assertEquals(2, context.dockerRunningCnt["agent"])
    }
}
