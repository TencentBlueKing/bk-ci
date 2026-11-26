package com.tencent.devops.dispatch

import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.dispatch.controller.BuildAgentBuildResourceImpl
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AskEnable
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskResp
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class BuildAgentAskThrottleTest {
    private val service = mockk<ThirdPartyAgentService>(relaxed = true)
    private val resource = BuildAgentBuildResourceImpl(service, mockk())
    private val passed = AgentResult<ThirdPartyAskResp>(0, null, null, null)
    private val info = ThirdPartyAskInfo(
        AskEnable("ALL", false, false, false),
        mockk<NewHeartbeatInfo>().also { every { it.agentIp } returns "10.0.0.1" },
        null
    )

    init {
        every { service.ask(any(), any(), any(), any()) } returns passed
    }

    @Test
    fun repeatedAskOnTheSameInstanceIsRejectedAndAlarmed() {
        assertSame(passed, resource.thirdPartyAgentAsk("p", "a", "s", info))
        val rejected = resource.thirdPartyAgentAsk("p", "a", "s", info)
        assertEquals(1, rejected.status)
        assertEquals("request too frequently", rejected.message)
        // 不同 agent 互不影响
        assertSame(passed, resource.thirdPartyAgentAsk("p", "b", "s", info))
        verify(exactly = 1) { service.ask("p", "a", "s", info) }
        verify(exactly = 1) { service.agentRepeatedInstallAlarm("p", "a", "10.0.0.1") }
    }

    @Test
    fun concurrentAsksOfOneAgentOnlyLetOneThrough() {
        val start = CountDownLatch(1)
        val delegated = AtomicInteger()
        every { service.ask(any(), "c", any(), any()) } answers {
            delegated.incrementAndGet()
            passed
        }
        (1..16).map {
            thread {
                start.await()
                resource.thirdPartyAgentAsk("p", "c", "s", info)
            }
        }.also { start.countDown() }.forEach { it.join() }
        assertEquals(1, delegated.get())
    }
}
