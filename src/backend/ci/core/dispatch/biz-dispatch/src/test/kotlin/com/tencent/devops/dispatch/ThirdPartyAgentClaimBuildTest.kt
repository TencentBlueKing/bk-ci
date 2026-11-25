package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AskEnable
import com.tencent.devops.dispatch.pojo.thirdpartyagent.BuildJobType
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskInfo
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.AskHeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.ExecutorService

class ThirdPartyAgentClaimBuildTest {
    // 不设任何应答：领取过程中只要访问 Redis（包括加锁）就会抛异常
    private val redis = mockk<RedisOperation>()
    private val client = mockk<Client>()
    private val environment = mockk<ServiceThirdPartyAgentResource>(relaxed = true)
    private val dao = mockk<ThirdPartyAgentBuildDao>()
    private val events = mockk<SampleEventDispatcher>(relaxed = true)
    private val agent = mockk<ThirdPartyAgent>()
    private val service = ThirdPartyAgentService(
        mockk(), mockk(), client, redis, dao, mockk(), mockk(), mockk(), events
    )
    private val ask = ThirdPartyAskInfo(AskEnable("ALL", false, false, false), mockk(), null)

    @BeforeEach
    fun prepare() {
        every { agent.secretKey } returns "secret"
        every { agent.status } returns AgentStatus.IMPORT_OK
        every { client.get(ServiceThirdPartyAgentResource::class) } returns environment
        every { environment.getAgentById("p", "a") } returns AgentResult(AgentStatus.IMPORT_OK, agent)
        every { environment.agentTaskStarted(any(), any(), any(), any(), any()) } returns Result(true)
        val heartbeat = mockk<AskHeartbeatResponse>()
        every { heartbeat.agentStatus } returns AgentStatus.IMPORT_OK.name
        every { environment.newHeartbeat("p", "a", any(), ask.heartbeat) } returns Result(heartbeat)
    }

    @AfterEach
    fun cleanup() {
        (ReflectionTestUtils.getField(service, "askExecutor") as ExecutorService).shutdownNow()
    }

    @Test
    fun claimsWithoutTouchingRedis() {
        every { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) } returns record(1)
        every { dao.claimQueueBuild(any(), 1, "a") } returns true

        assertEquals("b1", service.startBuild("p", "a", "secret", BuildJobType.ALL).data?.buildId)
        verify(exactly = 1) { environment.agentTaskStarted("p", "pipeline", "b1", "1", "a") }
        verify(exactly = 1) { events.dispatch(*anyVararg()) }
    }

    @Test
    fun lostClaimWaitsForTheNextAsk() {
        every { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) } returns record(1)
        every { dao.claimQueueBuild(any(), 1, "a") } returns false

        val result = service.startBuild("p", "a", "secret", BuildJobType.ALL)
        assertEquals(AgentStatus.IMPORT_OK, result.agentStatus)
        assertNull(result.data)
        // 每次领取最多一次查询加一次更新，抢输不重试
        verify(exactly = 1) { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) }
        verify(exactly = 0) { environment.agentTaskStarted(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { events.dispatch(*anyVararg()) }
    }

    @Test
    fun standaloneClaimChecksTheAgentEvenWhenTheQueueIsEmpty() {
        every { dao.fetchOneQueueBuild(any(), "a", BuildJobType.DOCKER) } returns null

        assertNull(service.startBuild("p", "a", "secret", BuildJobType.DOCKER).data)
        assertThrows(NotFoundException::class.java) { service.startBuild("p", "a", "revoked", BuildJobType.DOCKER) }
        // 独立领取接口靠返回值通知 agent 节点已删除
        every { environment.getAgentById("p", "a") } returns AgentResult(AgentStatus.DELETE, null)
        assertEquals(AgentStatus.DELETE, service.startBuild("p", "a", "secret", BuildJobType.DOCKER).agentStatus)
        // 后两次在校验 agent 时就返回，不查队列
        verify(exactly = 1) { dao.fetchOneQueueBuild(any(), "a", BuildJobType.DOCKER) }
        verify(exactly = 0) { dao.claimQueueBuild(any(), any(), any()) }
    }

    @Test
    fun askOnEmptyQueueSkipsTheAgentLookup() {
        every { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) } returns null

        val result = service.ask("p", "a", "secret", ask)
        assertEquals(AgentStatus.IMPORT_OK, result.agentStatus)
        assertNull(result.data?.build)
        verify(exactly = 1) { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) }
        verify(exactly = 0) { environment.getAgentById(any(), any()) }
    }

    @Test
    fun askChecksTheAgentBeforeClaiming() {
        every { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) } returns record(1)
        every { dao.claimQueueBuild(any(), 1, "a") } returns true

        assertNull(service.ask("p", "a", "revoked", ask).data?.build)
        verify(exactly = 0) { dao.claimQueueBuild(any(), any(), any()) }

        assertEquals("b1", service.ask("p", "a", "secret", ask).data?.build?.buildId)
        // 先查到任务再校验 agent，校验通过直接领取查到的这条，不再重复查询
        verify(exactly = 2) { environment.getAgentById("p", "a") }
        verify(exactly = 2) { dao.fetchOneQueueBuild(any(), "a", BuildJobType.ALL) }
        verify(exactly = 1) { dao.claimQueueBuild(any(), 1, "a") }
    }

    private fun record(id: Long) = TDispatchThirdpartyAgentBuildRecord().apply {
        this.id = id
        projectId = "p"
        agentId = "a"
        pipelineId = "pipeline"
        buildId = "b$id"
        vmSeqId = "1"
        workspace = ""
        status = PipelineTaskStatus.QUEUE.status
    }
}
